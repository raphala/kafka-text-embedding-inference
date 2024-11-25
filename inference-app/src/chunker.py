import logging

from paper import with_chunk, Paper

logging.basicConfig(level=logging.INFO)


def create_chunks(self, paper) -> list[Paper]:
    input_text = paper.abstract_()
    chunks = []
    text_length = len(input_text)
    start = 0

    while start < text_length:
        end = min(start + self.chunk_size, text_length)
        chunk = input_text[start:end]
        chunks.append(with_chunk(paper, chunk))
        start += self.chunk_size - self.chunk_overlap

    logging.info("created %i chunks from paper with abstract length %i", len(chunks), text_length)
    return chunks
