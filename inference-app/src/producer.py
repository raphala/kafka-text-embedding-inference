import json
import logging
import uuid

from confluent_kafka import SerializingProducer

from main import OUTPUT_TOPIC, BATCH_SIZE
from paper import Paper

logging.basicConfig(level=logging.INFO)


class Producer:

    def __init__(self, config):
        self.producer = SerializingProducer(config)
        self.producer.init_transactions()

    def produce_papers(self, papers: list[Paper], offsets, group_metadata):
        try:
            self.producer.begin_transaction()
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
                self.producer.poll(0)
                logging.info("producing vector from paper %s to topic %s", paper.title, OUTPUT_TOPIC)
                self.producer.produce(OUTPUT_TOPIC, json.dumps(qdrant_json).encode('utf-8'))

            self.producer.send_offsets_to_transaction(offsets, group_metadata)
            self.producer.commit_transaction()
            logging.info("produced %i vectors to topic %s", BATCH_SIZE, OUTPUT_TOPIC)
        except Exception as e:
            print(f"Transaction failed: {e}")
            self.producer.abort_transaction()

        finally:
            self.producer.flush()
