import asyncio
import uuid
from asyncio import Queue
from queue import Empty
from threading import Thread

from confluent_kafka import SerializingProducer
from confluent_kafka.serialization import SerializationContext, MessageField, StringSerializer

from logger import logger
from paper import Paper


class Record:
    def __init__(self, key, value):
        self.key = key
        self.value = value

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
                record = await self.queue.get()
                if record is None:
                    break

                self.producer.produce(topic=self.topic, key=record.key, value=record.value)
                self.producer.poll(0)

            except Empty:
                continue
            except Exception as e:
                logger.exception("Producer thread error", e)

    def produce_papers(self, paper: Paper):
        record = self.serialize_paper(paper)
        asyncio.run_coroutine_threadsafe(self.queue.put(record), self.loop)

    def serialize_paper(self, paper: Paper) -> Record:
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
        return Record(serialized_key, serialized_value)

    def shutdown(self):
        self.running = False
        self.queue.put(None)
        self.producer_thread.join()
        self.producer.flush()
