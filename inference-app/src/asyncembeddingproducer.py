import asyncio
import threading
import time
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
    def __init__(self, config, value_serializer, topic, batch_size=1000, flush_interval=32):
        self.producer = SerializingProducer(config)
        self.key_serializer = StringSerializer('utf_8')
        self.value_serializer = value_serializer
        self.topic = topic
        self.queue = asyncio.Queue()
        self.running = True

        self.batch_size = batch_size
        self.flush_interval = flush_interval
        self.messages_since_last_flush = 0
        self.last_flush_time = time.time()

        self.loop = asyncio.new_event_loop()
        self.producer_thread = Thread(target=self._run_event_loop, daemon=True)
        self.producer_thread.start()

    def _run_event_loop(self):
        asyncio.set_event_loop(self.loop)
        try:
            self.loop.run_until_complete(self._produce_from_queue())
        except Exception as e:
            logger.exception("Event loop error", exc_info=e)
        finally:
            self.loop.close()

    async def _produce_from_queue(self):
        while self.running:
            try:
                paper = await self.queue.get()
                if paper is None:
                    break

                record = self.serialize_paper(paper)
                self.producer.produce(
                    topic=self.topic,
                    key=record.key,
                    value=record.value,
                    on_delivery=self.delivery_report
                )
                self.messages_since_last_flush += 1

                current_time = time.time()
                time_since_last_flush = current_time - self.last_flush_time

                self.producer.poll(0)

                if (self.messages_since_last_flush >= self.batch_size or
                        time_since_last_flush >= self.flush_interval):
                    self.producer.flush(0)
                    self.messages_since_last_flush = 0
                    self.last_flush_time = current_time

            except Exception as e:
                logger.exception("Producer thread error", exc_info=e)

        # Flush any remaining messages before exiting
        self.producer.flush()

    def delivery_report(self, err, msg):
        if err is not None:
            logger.error(f"Delivery failed for record {msg.key()}: {err}")

    def produce_papers(self, paper: Paper):
        asyncio.run_coroutine_threadsafe(self.queue.put(paper), self.loop)

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
