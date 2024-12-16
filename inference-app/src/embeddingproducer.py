import uuid

from confluent_kafka import SerializingProducer
from confluent_kafka.serialization import SerializationContext, MessageField, StringSerializer

from logger import logger
from paper import Paper


class EmbeddingProducer:

    def __init__(self, config, value_serializer, topic):
        self.producer = SerializingProducer(config)
        self.key_serializer = StringSerializer('utf_8')
        self.value_serializer = value_serializer
        self.topic = topic
        self.pending_count = 0
        self.batch_size = 1000

    def produce_papers(self, paper: Paper):
        def delivery_report(err, msg):
            self.pending_count -= 1
            if err is not None:
                logger.error('Message delivery failed: {}'.format(err))

        try:
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
            serialized_value = self.value_serializer(qdrant_json,
                                                     SerializationContext(self.topic, MessageField.VALUE))
            self.pending_count += 1
            self.producer.produce(topic=self.topic, key=serialized_key, value=serialized_value, on_delivery=delivery_report)

            logger.info("Produced embedding to topic %s", self.topic)
        except Exception as e:
            logger.exception("Transaction failed", e)

        finally:
            if self.pending_count >= self.batch_size:
                logger.info("Flushing producer")
                self.producer.flush()
