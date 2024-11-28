import logging

from paper import with_chunk, Paper

logging.basicConfig(level=logging.INFO)


def create_chunks(paper, chunk_overlap, chunk_size) -> list[Paper]:
    input_text = paper.abstract
    chunks = []
    text_length = len(input_text)
    start = 0

    while start < text_length:
        end = min(start + chunk_size, text_length)
        chunk = input_text[start:end]
        chunks.append(with_chunk(paper, chunk))
        start += chunk_size - chunk_overlap

    return chunks
