import json
import logging
import uuid

from confluent_kafka import Producer

from main import PRODUCER_CONFIG, OUTPUT_TOPIC, BATCH_SIZE
from paper import Paper

logging.basicConfig(level=logging.INFO)

producer = Producer(PRODUCER_CONFIG)


def produce_papers(papers: list[Paper]):
    for paper in papers:
        qdrant_json = {
            "collection_name": "embedding",
            "id": str(uuid.uuid4()),
            "vector": paper.embedding_vector.tolist(),
            "payload": {
                "doi": paper.doi,
                "title": paper.title,
            }
        }
        producer.poll(0)
        logging.info("producing vector from paper %s to topic %s", paper.title, OUTPUT_TOPIC)
        producer.produce(OUTPUT_TOPIC, json.dumps(qdrant_json).encode('utf-8'))

    producer.flush()
    logging.info("produced %i vectors to topic %s", BATCH_SIZE, OUTPUT_TOPIC)
