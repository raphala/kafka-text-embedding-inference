from numpy import ndarray
from sentence_transformers import SentenceTransformer

def get_embedding(input_text: str) -> ndarray:
    return model.encode(input, convert_to_tensor=True, normalize_embeddings=True)

if __name__ == '__main__':
    model = SentenceTransformer('intfloat/multilingual-e5-large-instruct')