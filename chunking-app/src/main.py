import consumer

CHUNK_SIZE = 128
CHUNK_OVERLAP = 32
BOOTSTRAP_SERVER = 'my-cluster-kafka-bootstrap:9092'
INPUT_TOPIC = 'input'
OUTPUT_TOPIC = 'chunked'

CONSUMER_CONFIG = {
    'bootstrap.servers': BOOTSTRAP_SERVER,
    'group.id': 'chunking',
    'auto.offset.reset': 'earliest',
    'isolation.level': 'read_committed',
    'enable.auto.commit': False
}

PRODUCER_CONFIG = {
    'bootstrap.servers': BOOTSTRAP_SERVER,
    'transactional.id': 'chunking-producer-1'
}


def create_chunks(input_text: str) -> list[str]:
    chunks = []
    start = 0

    # TODO: fix last one
    while start <= len(input_text):
        end = start + CHUNK_SIZE
        chunks.append(input_text[start:end])
        start = end - CHUNK_OVERLAP

    return chunks


if __name__ == '__main__':
    consumer.run_consumer()
