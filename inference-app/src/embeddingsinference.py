import grpc
from typing import List
import numpy as np

# The generated proto files
import tei_pb2
import tei_pb2_grpc


class TextEmbeddingsClient:
    def __init__(self, server: str):
        self.channel = grpc.insecure_channel(server)
        self.embed_stub = tei_pb2_grpc.EmbedStub(self.channel)
        self.info_stub = tei_pb2_grpc.InfoStub(self.channel)

    def embed(self, text: str) -> np.ndarray:
        request = tei_pb2.EmbedRequest(inputs=text)

        response = self.embed_stub.Embed(request)
        return np.array(response.embeddings)

    def embed_batch(self, texts: List[str]) -> List[np.ndarray]:

        def generate_requests():
            for text in texts:
                yield tei_pb2.EmbedRequest(inputs=text)

        embeddings = []
        responses = self.embed_stub.EmbedStream(generate_requests())

        for response in responses:
            embeddings.append(np.array(response.embeddings))

        return embeddings

    def close(self):
        self.channel.close()
