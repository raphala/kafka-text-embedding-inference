from config import CHUNK_OVERLAP, CHUNK_SIZE
from paper import with_chunk, Paper


# TODO use tokens instead of char len!
def create_chunks(paper) -> list[Paper]:
    input_text = paper.abstract
    chunks = []
    text_length = len(input_text)
    start = 0

    while start < text_length:
        end = min(start + CHUNK_SIZE, text_length)
        chunk = input_text[start:end]
        chunks.append(with_chunk(paper, chunk))
        start += CHUNK_SIZE - CHUNK_OVERLAP

    return chunks
