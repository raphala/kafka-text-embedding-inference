import logging

import numpy as np

from main import BATCH_SIZE
from fastembed import TextEmbedding

logging.basicConfig(level=logging.INFO)

model = TextEmbedding(model_name="BAAI/bge-large-en-v1.5", providers=["CUDAExecutionProvider"])


def get_embedding(input_text: list[str]) -> list[np.ndarray]:
    logging.info("encoding %i embeddings", len(input_text))
    return model.encode(input_text, batch_size=BATCH_SIZE)
