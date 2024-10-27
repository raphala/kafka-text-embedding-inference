import json
import uuid

from confluent_kafka import Consumer, Producer

from model import model

import logging

# TODO exactly once - producer and consumer

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
            msg = consumer.poll(1.0)

            if msg is None:
                continue
            if msg.error():
                logging.error("Consumer error: %s", msg.error())
                continue
            decoded_value = msg.value().decode('utf-8')
            logging.info('Received message: %s', decoded_value)

            vector = model.get_embedding(decoded_value)
            # vector_json = json.dumps(vector.tolist())

            qdrant_json = {
                "collection_name": "embedding",
                "id": str(uuid.uuid4()),
                "vector": vector.tolist(),
                "payload": {
                    "name": "test",
                    "description": "this is a test",
                    "url": "https://test.org/"
                }
            }

            producer.poll(0)
            logging.info("producing vector")
            producer.produce(OUTPUT_TOPIC, json.dumps(qdrant_json).encode('utf-8'))


    finally:
        consumer.close()
        producer.flush()
