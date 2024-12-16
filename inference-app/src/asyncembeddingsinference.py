import asyncio
import threading
from asyncio import Queue, Future
from concurrent.futures import ThreadPoolExecutor
from threading import Thread

import grpc.aio
from typing import List, Any, Union
import numpy as np

# The generated proto files
import tei_pb2
import tei_pb2_grpc
from logger import logger
from asyncembeddingproducer import AsyncEmbeddingProducer
from paper import Paper, with_chunk


# class AsyncTextEmbeddingsClient:
#     def __init__(self, server: str, producer: AsyncEmbeddingProducer):
#         self.channel = grpc.insecure_channel(server)
#         self.embed_stub = tei_pb2_grpc.EmbedStub(self.channel)
#
#         # self.loop = asyncio.new_event_loop()
#         self.request_queue = Queue()
#         self.response_queue = Queue()
#         self.thread_pool = ThreadPoolExecutor(max_workers=4)
#         self.futures = {}
#         self.producer = producer
#         self.running = True
#         self.loop = asyncio.new_event_loop()
#
#         self._stream_thread = threading.Thread(target=self._run_stream)
#         self._stream_thread.daemon = True
#         self._stream_thread.start()
#         self.stream_loop = asyncio.new_event_loop()
#
#         self._response_thread = threading.Thread(target=self._process_responses)
#         self._response_thread.daemon = True
#         self._response_thread.start()
#         self.response_loop = asyncio.new_event_loop()
#
#     def _run_stream(self):
#         asyncio.set_event_loop(self.stream_loop)
#
#         def request_iterator():
#             while self.running:
#                 try:
#                     request_id, text = asyncio.run_coroutine_threadsafe(self.request_queue.get(), self.loop)
#                     # request_id, text = self.request_queue.get()
#                     logger.info("Sending embed request")
#                     yield tei_pb2.EmbedRequest(inputs=text)
#                 except:
#                     continue
#
#         while self.running:
#             try:
#                 for response in self.embed_stub.EmbedStream(request_iterator()):
#                     logger.info("Putting response in queue")
#                     asyncio.run_coroutine_threadsafe(self.response_queue.put(response), self.stream_loop)
#                     # self.response_queue.put(response)
#             except Exception as e:
#                 logger.error(f"Stream error: {e}")
#                 if self.running:
#                     continue
#
#     def _process_responses(self):
#         asyncio.set_event_loop(self.response_loop)
#
#         while self.running:
#             try:
#                 response = asyncio.run_coroutine_threadsafe(self.response_queue.get(), self.response_loop)
#                 # response = self.response_queue.get()
#                 request_id, future = self.futures.popitem()
#                 logger.info("Received response", response.embeddings)
#                 # future.set_result(list(response.embeddings))
#                 # self.producer.produce_papers(with_chunk(paper, response.embeddings))
#             except:
#                 continue
#
#     def embed(self, text: str) -> Union[List[float], Future]:
#         future = self.thread_pool.submit(lambda: [])  # placeholder
#         request_id = id(future)
#
#         self.futures[request_id] = future
#
#         logger.info("Putting paper in request queue")
#         asyncio.run_coroutine_threadsafe(self.request_queue.put((request_id, text)), self.loop)
#
#         return future
#
#     def close(self):
#         self.channel.close()

class AsyncTextEmbeddingsClient:
    def __init__(self, server: str, producer: AsyncEmbeddingProducer):
        self.channel = grpc.insecure_channel(server)
        self.stub = tei_pb2_grpc.EmbedStub(self.channel)
        self.loop = asyncio.new_event_loop()
        self.loop_thread = threading.Thread(target=self.start_loop, daemon=True)
        self.loop_thread.start()
        self.producer = producer
        self.semaphore = threading.Semaphore(500)

    def start_loop(self):
        asyncio.set_event_loop(self.loop)
        self.loop.run_forever()

    def get_embedding(self, paper):
        """
        Synchronous method to get embeddings for a given text.
        """
        def produce_embeddings(_future):
            try:
                result = _future.result()
                # paper.embedding_vector = np.array(result.embeddings, dtype=np.float32)
                paper.embedding_vector = np.round(np.array(result.embeddings, dtype=float), decimals=7).tolist()
                self.producer.produce_papers(paper)
            except Exception as e:
                logger.error("Error processing future: %s", e)
            finally:
                self.semaphore.release()

        self.semaphore.acquire()
        future = self.stub.Embed.future(tei_pb2.EmbedRequest(inputs=paper.text_chunk))
        future.add_done_callback(produce_embeddings)

    def close(self):
        """
        Clean up the client by closing the channel and stopping the event loop.
        """
        self.loop.call_soon_threadsafe(self.loop.stop)
        self.loop_thread.join()
        self.channel.close()

