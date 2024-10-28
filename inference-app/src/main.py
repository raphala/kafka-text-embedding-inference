import json
import uuid

from confluent_kafka import Consumer, Producer

from model import model

import logging
from paper import Paper

# TODO exactly once - paper-producer and consumer

logging.basicConfig(level=logging.INFO)

BOOTSTRAP_SERVER = 'my-cluster-kafka-bootstrap:9092'
INPUT_TOPIC = 'input'
OUTPUT_TOPIC = 'embedding'

CONSUMER_CONFIG = {
    'bootstrap.servers': BOOTSTRAP_SERVER,
    'group.id': 'embeddings',
    'auto.offset.reset': 'earliest'
}

PRODUCER_CONFIG = {
    'bootstrap.servers': BOOTSTRAP_SERVER
}

if __name__ == '__main__':
    consumer = Consumer(CONSUMER_CONFIG)
    consumer.subscribe([INPUT_TOPIC])

    producer = Producer(PRODUCER_CONFIG)

    try:
        while True:
            messages = consumer.consume(num_messages=model.BATCH_SIZE, timeout=1)
            if len(messages) == 0:
                continue

            papers = []
            for message in messages:
                if message is None:
                    continue
                if message.error():
                    logging.error("Consumer error: %s", messages.error())
                    continue
                decoded_json = json.loads(message.value().decode('utf-8'))

                doi = decoded_json.get('doi', '')
                title = decoded_json.get('title', '')
                abstract = decoded_json.get('abstract', '')
                paper = Paper(doi, title, abstract)
                papers.append(paper)

                logging.info('Received message %s with title %s', doi, title)

            abstract_list = [paper.abstract for paper in papers]
            vectors = model.get_embedding(abstract_list)

            for i in range(len(papers)):
                paper = papers[i]
                vector = vectors[i].tolist()
                qdrant_json = {
                    "collection_name": "embedding",
                    "id": str(uuid.uuid4()),
                    "vector": vector,
                    "payload": {
                        "doi": paper.doi,
                        "title": paper.title,
                    }
                }

                producer.poll(0)
                logging.info("producing vector from paper %s to topic %s", paper.title, OUTPUT_TOPIC)
                producer.produce(OUTPUT_TOPIC, json.dumps(qdrant_json).encode('utf-8'))
            logging.info("produced %i vectors to topic %s", model.BATCH_SIZE, OUTPUT_TOPIC)


    finally:
        consumer.close()
        producer.flush()
