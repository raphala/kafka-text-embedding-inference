import json

import requests
from confluent_kafka import Producer

from paper.paper import extract_papers_from_json

import logging

logging.basicConfig(level=logging.INFO)

BOOTSTRAP_SERVER = 'my-cluster-kafka-bootstrap:9092'
OUTPUT_TOPIC = 'input'

PAPER_URL = "https://www.ebi.ac.uk/europepmc/webservices/rest/search?query=*&resultType=core&pageSize=1000&format=json"
CURSOR_MARK = "&cursorMark="

PAGES = 100

PRODUCER_CONFIG = {
    'bootstrap.servers': BOOTSTRAP_SERVER
}


def request_papers(url: str):
    try:
        response = requests.get(url)
        response.raise_for_status()
        data = json.dumps(response.json())
        return data

    except:
        print(f"Error while fetching papers")


def get_papers(next_cursor: str) -> tuple[str, list]:
    data = request_papers(PAPER_URL + CURSOR_MARK + next_cursor)
    return extract_papers_from_json(data)


if __name__ == '__main__':
    producer = Producer(PRODUCER_CONFIG)

    current_cursor = "*"
    for i in range(PAGES):
        current_cursor, papers = get_papers(current_cursor)
        producer.poll(0)
        for paper in papers:
            producer.produce(topic=OUTPUT_TOPIC, key=paper.doi, value=paper.to_json())

        logging.info("Produced 1000 papers to %s - %i/%i", OUTPUT_TOPIC, i, PAGES)

    producer.flush()
