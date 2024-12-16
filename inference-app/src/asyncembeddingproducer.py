import asyncio
import uuid
from asyncio import Queue
from queue import Empty
from threading import Thread

from confluent_kafka import SerializingProducer
from confluent_kafka.serialization import SerializationContext, MessageField, StringSerializer

from logger import logger
from paper import Paper


class AsyncEmbeddingProducer:
    def __init__(self, config, value_serializer, topic):
        self.producer = SerializingProducer(config)
        self.key_serializer = StringSerializer('utf_8')
        self.value_serializer = value_serializer
        self.topic = topic
        self.queue = Queue()
        self.running = True

        self.loop = asyncio.new_event_loop()
        self.producer_thread = Thread(target=self._run_event_loop, daemon=True)
        self.producer_thread.start()

    def _run_event_loop(self):
        asyncio.set_event_loop(self.loop)
        self.loop.run_until_complete(self._produce_from_queue())

    async def _produce_from_queue(self):
        while self.running:
            try:
                paper = await self.queue.get()
                if paper is None:
                    break

                embedding_id = str(uuid.uuid4())
                qdrant_json = {
                    "collection_name": "embedding",
                    "id": embedding_id,
                    "vector": paper.embedding_vector,
                    "payload": {
                        "doi": paper.doi,
                        "title": paper.title,
                        "abstract_chunk": paper.text_chunk
                    }
                }
                serialized_key = self.key_serializer(embedding_id,
                                                     SerializationContext(self.topic, MessageField.VALUE))
                serialized_value = self.value_serializer(qdrant_json,
                                                         SerializationContext(self.topic, MessageField.VALUE))
                self.producer.poll(0)
                self.producer.produce(topic=self.topic, key=serialized_key, value=serialized_value)
                self.producer.flush()

            except Empty:
                continue
            except Exception as e:
                logger.exception("Producer thread error", e)

    def produce_papers(self, paper: Paper):
        asyncio.run_coroutine_threadsafe(self.queue.put(paper), self.loop)

    def shutdown(self):
        self.running = False
        self.queue.put(None)
        self.producer_thread.join()
        self.producer.flush()
