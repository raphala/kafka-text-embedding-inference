class Paper:
    def __init__(self, doi, title, abstract):
        self.doi = doi
        self.title = title
        self.abstract = abstract
        self.embedding_vector = None

    def __str__(self):
        return f"Paper: {self.title}\nDOI: {self.doi}\nAbstract: {self.abstract[:100]}..."
