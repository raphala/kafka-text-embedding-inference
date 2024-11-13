import logging

import consumer

# TODO exactly once - paper-producer and consumer
# TODO error handling - dead letter
# TODO prepare deployment
# TODO avro/protobuf support
# TODO autoscaling - scaling with transactional api?

logging.basicConfig(level=logging.INFO)

BOOTSTRAP_SERVER = 'my-cluster-kafka-bootstrap:9092'
INPUT_TOPIC = 'chunked'
OUTPUT_TOPIC = 'embedding'
BATCH_SIZE = 64

CONSUMER_CONFIG = {
    'bootstrap.servers': BOOTSTRAP_SERVER,
    'group.id': 'embeddings',
    'auto.offset.reset': 'earliest',
    'isolation.level': 'read_committed',
    'enable.auto.commit': False
}

PRODUCER_CONFIG = {
    'bootstrap.servers': BOOTSTRAP_SERVER,
    'transactional.id': 'embeddings-producer-1'
}

if __name__ == '__main__':
    consumer.run_consumer()
