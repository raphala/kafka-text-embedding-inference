class Paper:
    def __init__(self, doi, title, abstract):
        self.doi = doi
        self.title = title
        self.abstract = abstract
        self.text_chunk = None
        self.embedding_vector = None

    def __str__(self):
        return f"Paper: {self.title}\nDOI: {self.doi}\nAbstract: {self.abstract[:100]}..."


def dict_to_paper(paper, ctx):
    if paper is None:
        return None

    return Paper(doi=paper['doi'],
                 title=paper['title'],
                 abstract=paper['abstract'])


def with_chunk(paper, text_chunk) -> Paper:
    chunked_paper = Paper(doi=paper.doi, title=paper.title, abstract=paper.abstract)
    chunked_paper.text_chunk = text_chunk
    return chunked_paper
