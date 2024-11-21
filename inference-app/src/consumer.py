import json
import logging

from confluent_kafka import Consumer, TopicPartition

import model
import producer
from main import CONSUMER_CONFIG, INPUT_TOPIC, BATCH_SIZE
from paper import Paper
from chunker import create_chunks

logging.basicConfig(level=logging.INFO)


def run_consumer():
    consumer = Consumer(CONSUMER_CONFIG)
    consumer.subscribe([INPUT_TOPIC])

    try:
        while True:
            messages = consumer.consume(num_messages=BATCH_SIZE, timeout=1)
            if len(messages) == 0:
                continue

            papers = extract_papers(messages)
            chunked_papers = [create_chunks(paper) for paper in papers]
            inferred_papers = infer_embeddings(chunked_papers)
            group_metadata = consumer.consumer_group_metadata()
            producer.produce_papers(inferred_papers, get_offsets(messages), group_metadata)
    except Exception as e:
        print(f"Consumption failed: {e}")

    finally:
        consumer.close()


def extract_papers(messages) -> list[Paper]:
    papers = []
    for message in messages:
        if message is None:
            continue
        if message.error():
            logging.error("Consumer error: %s", messages.error())
            continue
        decoded_json = json.loads(message.value().decode('utf-8'))

        doi = decoded_json.get('doi', '')
        title = decoded_json.get('title', '')
        abstract = decoded_json.get('abstract', '')
        paper = Paper(doi, title, abstract)
        papers.append(paper)

        logging.info('Received message %s with title %s', doi, title)
    return papers


def infer_embeddings(papers: list[Paper]) -> list[Paper]:
    abstract_list = [paper.abstract for paper in papers]
    vectors = model.get_embedding(abstract_list)
    for i in range(len(papers)):
        papers[i].embedding_vector = vectors[i]
    return papers


def get_offsets(messages):
    offsets = []
    for message in messages:
        tp = TopicPartition(message.topic(), message.partition(), message.offset() + 1)
        offsets.append(tp)
    return offsets
