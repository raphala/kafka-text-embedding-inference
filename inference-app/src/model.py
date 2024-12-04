import numpy as np
from fastembed import TextEmbedding

from config import EXECUTION_PROVIDERS, EMBEDDING_MODEL
from logger import logger

try:
    logger.info("Loading embedding model %s with execution provider %s", EMBEDDING_MODEL, EXECUTION_PROVIDERS)
    model = TextEmbedding(model_name=EMBEDDING_MODEL, providers=EXECUTION_PROVIDERS)
    logger.info("Successfully loaded embedding model")
except Exception as e:
    logger.error("Error loading model", e)


def get_embedding(input_text: list[str]) -> list[float]:
    logger.info("Encoding %i embeddings", len(input_text))
    embeddings = model.embed(input_text)
    return np.concatenate(embeddings).astype(float).tolist()
