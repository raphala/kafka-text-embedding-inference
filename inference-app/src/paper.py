class Paper:
    def __init__(self, doi, title, abstract):
        self.doi = doi
        self.title = title
        self.abstract = abstract

    def __str__(self):
        return f"Paper: {self.title}\nDOI: {self.doi}\nAbstract: {self.abstract[:100]}..."

    def get_doi(self):
        return self.doi

    def get_title(self):
        return self.title

    def get_abstract(self):
        return self.abstract

    def set_doi(self, new_doi):
        self.doi = new_doi

    def set_title(self, new_title):
        self.title = new_title

    def set_abstract(self, new_abstract):
        self.abstract = new_abstract
