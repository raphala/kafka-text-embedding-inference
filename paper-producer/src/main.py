import json

import requests
from confluent_kafka import Producer

from paper.paper import extract_papers_from_json

import logging

logging.basicConfig(level=logging.INFO)

BOOTSTRAP_SERVER = 'my-cluster-kafka-bootstrap:9092'
OUTPUT_TOPIC = 'input'

PAPER_URL = "https://www.ebi.ac.uk/europepmc/webservices/rest/search?query=*&resultType=core&cursorMark=*&pageSize=1000&format=json"

COUNT = 1000

PRODUCER_CONFIG = {
    'bootstrap.servers': BOOTSTRAP_SERVER
}


def request_papers():
    try:
        response = requests.get(PAPER_URL)
        response.raise_for_status()
        data = json.dumps(response.json())
        return data

    except:
        print(f"Error while fetching papers")


def get_papers():
    data = request_papers()
    return extract_papers_from_json(data)


if __name__ == '__main__':
    producer = Producer(PRODUCER_CONFIG)

    papers = get_papers()
    # for paper in papers:
    for i in range(COUNT):
        paper = papers[i]
        producer.poll(0)
        producer.produce(topic=OUTPUT_TOPIC, key=paper.doi, value=paper.to_json())
        logging.info("Produced paper %i to %s", i, OUTPUT_TOPIC)

    producer.flush()
