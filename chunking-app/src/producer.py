import json
import logging

from confluent_kafka import Producer

from main import PRODUCER_CONFIG, OUTPUT_TOPIC

logging.basicConfig(level=logging.INFO)

producer = Producer(PRODUCER_CONFIG)
producer.init_transactions()


def produce_chunks(chunks: list[str], offsets, group_metadata):
    try:
        producer.begin_transaction()
        for chunk in chunks:
            producer.poll(0)
            producer.produce(OUTPUT_TOPIC, json.dumps(chunk).encode('utf-8'))

        producer.send_offsets_to_transaction(offsets, group_metadata)
        producer.commit_transaction()
        logging.info("produced %i chunks from abstract x to topic %s", len(chunks), OUTPUT_TOPIC)
    except Exception as e:
        print(f"Transaction failed: {e}")
        producer.abort_transaction()

    finally:
        producer.flush()
