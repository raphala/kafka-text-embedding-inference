import logging

import numpy as np
from fastembed import TextEmbedding

from main import EXECUTION_PROVIDER

logging.basicConfig(level=logging.INFO)

logging.info("Using provider: %s", EXECUTION_PROVIDER)
model = TextEmbedding(model_name="sentence-transformers/all-MiniLM-L6-v2", providers=[EXECUTION_PROVIDER])


def get_embedding(input_text: list[str]) -> list[np.ndarray]:
    logging.info("encoding %i embeddings", len(input_text))
    return model.embed(input_text)
