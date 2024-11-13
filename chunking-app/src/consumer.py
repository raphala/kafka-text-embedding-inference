import json
import logging

from confluent_kafka import Consumer, TopicPartition

import producer
from main import CONSUMER_CONFIG, INPUT_TOPIC, create_chunks

logging.basicConfig(level=logging.INFO)


def run_consumer():
    consumer = Consumer(CONSUMER_CONFIG)
    consumer.subscribe([INPUT_TOPIC])

    try:
        while True:
            message = consumer.poll(timeout=1.0)
            if message is None: continue

            paper = json.loads(message.value().decode('utf-8'))
            full_abstract = paper.get('abstract', '')
            chunks = create_chunks(full_abstract)
            group_metadata = consumer.consumer_group_metadata()
            producer.produce_chunks(chunks, get_offsets(message), group_metadata)
    except Exception as e:
        print(f"Consumption failed: {e}")

    finally:
        consumer.close()


def get_offsets(message):
    return [TopicPartition(message.topic(), message.partition(), message.offset() + 1)]
