class Paper:
    def __init__(self, doi, title, abstract):
        self.doi = doi
        self.title = title
        self.abstract = abstract
        self.text_chunk = None
        self.embedding_vector = None

    def __str__(self):
        return f"Paper: {self.title}\nDOI: {self.doi}\nAbstract: {self.abstract[:100]}..."


def with_chunk(paper, text_chunk) -> Paper:
    return Paper(doi=paper.doi, title=paper.title, abstract=paper.abstract, text_chunk=text_chunk)
