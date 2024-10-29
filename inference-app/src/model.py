import logging

from sentence_transformers import SentenceTransformer
from torch import Tensor

from main import BATCH_SIZE

logging.basicConfig(level=logging.INFO)

model = SentenceTransformer('sentence-transformers/all-MiniLM-L6-v2')


# model = SentenceTransformer('intfloat/multilingual-e5-large-instruct')

def get_embedding(input_text: str) -> Tensor:
    logging.info("encoding embedding for %s", input_text)
    return model.encode(input_text, batch_size=BATCH_SIZE)
