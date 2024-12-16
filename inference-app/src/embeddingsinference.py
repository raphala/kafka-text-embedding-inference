import grpc.aio
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

    async def embed(self, text: str) -> np.ndarray:
        request = tei_pb2.EmbedRequest(inputs=text)

        response = await self.embed_stub.Embed(request)
        return np.array(response.embeddings, dtype=np.float32)

    # def embed_batch(self, texts: List[str]) -> List[float]:
    #
    #     def generate_requests():
    #         for text in texts:
    #             yield tei_pb2.EmbedRequest(inputs=text)
    #
    #     embeddings = []
    #     responses = self.embed_stub.EmbedStream(generate_requests())
    #
    #     for response in responses:
    #         raw_embeddings = np.array(response.embeddings)
    #         quantized_embeddings = raw_embeddings.astype(np.float16)
    #         # quantized_embeddings = quantizer.quantize(raw_embeddings)
    #         embeddings.append(raw_embeddings)
    #
    #     return embeddings

    def close(self):
        self.channel.close()
