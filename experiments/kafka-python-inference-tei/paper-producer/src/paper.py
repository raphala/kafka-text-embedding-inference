import json


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

    def to_json(self):
        paper_dict = {
            "title": self.title,
            "abstract": self.abstract,
            "doi": self.doi
        }
        return json.dumps(paper_dict)

    def to_dict(self) -> dict:
        return {
            "title": self.title,
            "abstract": self.abstract,
            "doi": self.doi
        }


def extract_papers_from_json(json_data) -> tuple[str, list]:
    papers = []
    data = json.loads(json_data)

    next_cursor_mark = data.get('nextCursorMark', '')

    # Extract the result list
    results = data.get('resultList', {}).get('result', [])

    for result in results:
        doi = result.get('doi', '')
        title = result.get('title', '')
        abstract = result.get('abstractText', '')

        paper = Paper(doi, title, abstract)
        papers.append(paper)

    return next_cursor_mark, papers
