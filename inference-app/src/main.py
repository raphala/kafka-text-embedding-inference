import logging

import consumer

# TODO exactly once - paper-producer and consumer
# TODO error handling - dead letter

logging.basicConfig(level=logging.INFO)

BOOTSTRAP_SERVER = 'my-cluster-kafka-bootstrap:9092'
INPUT_TOPIC = 'input'
OUTPUT_TOPIC = 'embedding'
BATCH_SIZE = 64

CONSUMER_CONFIG = {
    'bootstrap.servers': BOOTSTRAP_SERVER,
    'group.id': 'embeddings',
    'auto.offset.reset': 'earliest'
}

PRODUCER_CONFIG = {
    'bootstrap.servers': BOOTSTRAP_SERVER
}

if __name__ == '__main__':
    consumer.run_consumer()
