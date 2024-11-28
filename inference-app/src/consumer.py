import logging

from confluent_kafka import TopicPartition, Consumer
from confluent_kafka.serialization import SerializationContext, MessageField

import model
from chunker import create_chunks
from paper import Paper

logging.basicConfig(level=logging.INFO)


def run_consumer(config, producer, input_topic, batch_size, chunk_overlap, chunk_size, value_deserializer):
    consumer = Consumer(config)
    consumer.subscribe([input_topic])

    try:
        while True:
            # TODO has to use poll in order to work with DeserializingConsumer
            # TODO idea: make custom deserialization consumer
            messages = consumer.consume(num_messages=batch_size, timeout=1)
            logging.info("Consumed %i messages from topic %s", len(messages), input_topic)
            if len(messages) == 0:
                continue

            papers = extract_papers(messages, value_deserializer)
            paper_chunks = []
            for paper in papers:
                chunks = create_chunks(paper, chunk_overlap, chunk_size)
                paper_chunks.extend(chunks)

            inferred_papers = infer_embeddings(paper_chunks)

            group_metadata = consumer.consumer_group_metadata()
            producer.produce_papers(inferred_papers, get_offsets(messages), group_metadata)
    except Exception as e:
        logging.exception(e)

    finally:
        consumer.close()


def extract_papers(messages, value_deserializer) -> list[Paper]:
    papers = []
    for message in messages:
        if message is None:
            continue
        if message.error():
            logging.error("Consumer error: %s", messages.error())
            continue

        ctx = SerializationContext(message.topic(), MessageField.VALUE, message.headers())
        paper = value_deserializer(message.value(), ctx)

        papers.append(paper)

    return papers


def infer_embeddings(papers: list[Paper]) -> list[Paper]:
    abstract_list = [paper.abstract for paper in papers]
    logging.info("encoding embedding for %i paper chunks", len(papers))
    vectors = list(model.get_embedding(abstract_list))
    for i, vector in enumerate(vectors):
        papers[i].embedding_vector = vector
    return papers


def get_offsets(messages):
    offsets = []
    for message in messages:
        tp = TopicPartition(message.topic(), message.partition(), message.offset() + 1)
        offsets.append(tp)
    return offsets
