import logging
import uuid

from confluent_kafka import SerializingProducer
from confluent_kafka.serialization import SerializationContext, MessageField, StringSerializer

from paper import Paper

logging.basicConfig(level=logging.INFO)


class EmbeddingProducer:

    def __init__(self, config, value_serializer, topic):
        self.producer = SerializingProducer(config)
        self.producer.init_transactions()
        self.key_serializer = StringSerializer('utf_8')
        self.value_serializer = value_serializer
        self.topic = topic

    def produce_papers(self, papers: list[Paper], offsets, group_metadata):
        try:
            self.producer.begin_transaction()
            for paper in papers:
                embedding_id = str(uuid.uuid4())
                qdrant_json = {
                    "collection_name": "embedding",
                    "id": embedding_id,
                    "vector": paper.embedding_vector.tolist(),
                    "payload": {
                        "doi": paper.doi,
                        "title": paper.title,
                        "abstract_chunk": paper.text_chunk
                    }
                }
                serialized_key = self.key_serializer(embedding_id, SerializationContext(self.topic, MessageField.VALUE))
                serialized_value = self.value_serializer(qdrant_json, SerializationContext(self.topic, MessageField.VALUE))
                self.producer.poll(0)
                self.producer.produce(topic=self.topic, key=serialized_key, value=serialized_value)

            self.producer.send_offsets_to_transaction(offsets, group_metadata)
            self.producer.commit_transaction()
            logging.info("produced %i vectors to topic %s", len(papers), self.topic)
        except Exception as e:
            logging.exception("Transaction failed", e)
            self.producer.abort_transaction()

        finally:
            self.producer.flush()
