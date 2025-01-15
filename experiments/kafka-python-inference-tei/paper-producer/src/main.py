import json
import logging
import os
from pathlib import Path

import requests
from confluent_kafka import SerializingProducer, Producer
from confluent_kafka.schema_registry import SchemaRegistryClient
from confluent_kafka.schema_registry.json_schema import JSONSerializer
from confluent_kafka.serialization import StringSerializer

from paper import extract_papers_from_json

logging.basicConfig(level=logging.INFO)

SCHEMA_REGISTRY = os.environ.get("SCHEMA_REGISTRY", None)
BOOTSTRAP_SERVER = os.environ.get("BOOTSTRAP_SERVER", "localhost:9092")
OUTPUT_TOPIC = os.environ.get("OUTPUT_TOPIC", "paper")
PAGES_COUNT = int(os.environ.get("PAGES_COUNT", 50))

PAPER_URL = "https://www.ebi.ac.uk/europepmc/webservices/rest/search?query=*&resultType=core&pageSize=1000&format=json"
CURSOR_MARK = "&cursorMark="

PROJECT_ROOT = Path(__file__).parent.parent.parent


def request_papers(url: str):
    try:
        response = requests.get(url)
        response.raise_for_status()
        data = json.dumps(response.json())
        return data

    except:
        print(f"Error while fetching papers")


def load_schema(schema_name: str) -> dict:
    schema_path = PROJECT_ROOT / "schemas" / schema_name
    try:
        with open(schema_path) as f:
            return json.load(f)
    except FileNotFoundError:
        raise FileNotFoundError(f"Schema file not found at {schema_path}")


def get_papers(next_cursor: str) -> tuple[str, list]:
    data = request_papers(PAPER_URL + CURSOR_MARK + next_cursor)
    return extract_papers_from_json(data)


if __name__ == '__main__':
    if SCHEMA_REGISTRY is None:
        producer_config = {
            'bootstrap.servers': BOOTSTRAP_SERVER
        }
        producer = Producer(producer_config)
    else:
        schema = load_schema("paper.json")
        schema_str = json.dumps(schema)
        schema_registry_conf = {'url': SCHEMA_REGISTRY}
        schema_registry_client = SchemaRegistryClient(schema_registry_conf)
        json_serializer = JSONSerializer(schema_str, schema_registry_client)
        string_serializer = StringSerializer('utf_8')

        producer_config = {
            'bootstrap.servers': BOOTSTRAP_SERVER,
            'key.serializer': string_serializer,
            'value.serializer': json_serializer
        }
        producer = SerializingProducer(producer_config)

    current_cursor = "*"
    for i in range(PAGES_COUNT):
        current_cursor, papers = get_papers(current_cursor)
        producer.poll(0)
        for paper in papers:
            if SCHEMA_REGISTRY is None:
                producer.produce(topic=OUTPUT_TOPIC, key=paper.doi, value=json.dumps(paper.to_dict()).encode('utf-8'))
            else:
                producer.produce(topic=OUTPUT_TOPIC, key=paper.doi, value=paper.to_dict())

        logging.info("Produced 1000 papers to %s - %i/%i", OUTPUT_TOPIC, i + 1, PAGES_COUNT)

    producer.flush()
