from confluent_kafka import TopicPartition, Consumer
from confluent_kafka.serialization import SerializationContext, MessageField

import model
from chunker import create_chunks
from config import INPUT_TOPIC, BATCH_SIZE
from logger import logger
from paper import Paper


def run_consumer(config, producer, value_deserializer):
    consumer = Consumer(config)
    consumer.subscribe([INPUT_TOPIC])

    try:
        while True:
            # TODO has to use poll in order to work with DeserializingConsumer
            # TODO idea: make custom deserialization consumer
            messages = consumer.consume(num_messages=BATCH_SIZE, timeout=1)
            logger.info("Consumed %i messages from topic %s", len(messages), INPUT_TOPIC)
            if len(messages) == 0:
                continue

            papers = extract_papers(messages, value_deserializer)
            paper_chunks = []
            for paper in papers:
                chunks = create_chunks(paper)
                paper_chunks.extend(chunks)

            inferred_papers = infer_embeddings(paper_chunks)

            group_metadata = consumer.consumer_group_metadata()
            producer.produce_papers(inferred_papers, get_offsets(messages), group_metadata)
    except Exception as e:
        logger.exception(e)

    finally:
        consumer.close()


def extract_papers(messages, value_deserializer) -> list[Paper]:
    papers = []
    for message in messages:
        if message is None:
            continue
        if message.error():
            logger.error("Consumer error: %s", messages.error())
            continue

        ctx = SerializationContext(message.topic(), MessageField.VALUE, message.headers())
        paper = value_deserializer(message.value(), ctx)

        papers.append(paper)

    return papers


def infer_embeddings(papers: list[Paper]) -> list[Paper]:
    abstract_list = [paper.abstract for paper in papers]
    logger.info("encoding embedding for %i paper chunks", len(papers))
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
