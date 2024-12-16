from time import perf_counter

from confluent_kafka import Consumer
from confluent_kafka.serialization import SerializationContext, MessageField

from asyncembeddingsinference import AsyncTextEmbeddingsClient
from chunker import create_chunks
from config import INPUT_TOPIC, BATCH_SIZE, TEI_SERVER
from logger import logger
from paper import Paper


def run_consumer(config, producer, value_deserializer):
    consumer = Consumer(config)
    consumer.subscribe([INPUT_TOPIC])
    embeddings_inference = AsyncTextEmbeddingsClient(TEI_SERVER, producer)

    try:
        while True:
            # TODO has to use poll in order to work with DeserializingConsumer
            # TODO idea: make custom deserialization consumer
            consumer_start_time = perf_counter()
            messages = consumer.consume(num_messages=BATCH_SIZE, timeout=1)
            consumer_end_time = perf_counter()
            logger.info("Consumed %i messages from topic %s took %f seconds", len(messages), INPUT_TOPIC,
                        consumer_end_time - consumer_start_time)
            if len(messages) == 0:
                continue

            chunking_start_time = perf_counter()
            papers = extract_papers(messages, value_deserializer)
            paper_chunks = []
            for paper in papers:
                chunks = create_chunks(paper)
                paper_chunks.extend(chunks)

            chunking_end_time = perf_counter()
            logger.info("Extracting and chunking papers took %f seconds", chunking_end_time - chunking_start_time)

            embedding_start_time = perf_counter()
            for paper_chunk in paper_chunks:
                embeddings_inference.produce_embedding(paper_chunk)
            embedding_end_time = perf_counter()
            logger.info("Embedding and producing papers took %f seconds", embedding_end_time - embedding_start_time)
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
