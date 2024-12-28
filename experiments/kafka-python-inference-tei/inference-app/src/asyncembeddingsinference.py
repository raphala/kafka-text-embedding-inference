import threading

import grpc.aio
import numpy as np

import tei_pb2
import tei_pb2_grpc
from asyncembeddingproducer import AsyncEmbeddingProducer
from config import MAX_CONCURRENT_REQUESTS
from logger import logger


class AsyncTextEmbeddingsClient:
    def __init__(self, server: str, producer: AsyncEmbeddingProducer):
        self.channel = grpc.insecure_channel(server)
        self.stub = tei_pb2_grpc.EmbedStub(self.channel)
        self.producer = producer
        self.semaphore = threading.Semaphore(MAX_CONCURRENT_REQUESTS)

    def produce_embedding(self, paper):
        def produce_embeddings(_future):
            try:
                result = _future.result()
                paper.embedding_vector = np.round(np.array(result.embeddings, dtype=float), decimals=8).tolist()
                self.producer.produce_papers(paper)
            except Exception as e:
                logger.error("Error processing future: %s", e)
            finally:
                self.semaphore.release()

        self.semaphore.acquire()
        future = self.stub.Embed.future(tei_pb2.EmbedRequest(inputs=paper.text_chunk, truncate=True))
        future.add_done_callback(produce_embeddings)

    def close(self):
        self.channel.close()
