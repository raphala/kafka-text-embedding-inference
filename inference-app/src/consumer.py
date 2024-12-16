import asyncio

from confluent_kafka import TopicPartition, Consumer
from confluent_kafka.serialization import SerializationContext, MessageField

from chunker import create_chunks
from config import INPUT_TOPIC, BATCH_SIZE, TEI_SERVER
from logger import logger
from paper import Paper
from time import perf_counter
from asyncembeddingsinference import AsyncTextEmbeddingsClient
# from embeddingsinference import TextEmbeddingsClient


def run_consumer(config, producer, value_deserializer):
    consumer = Consumer(config)
    consumer.subscribe([INPUT_TOPIC])
    # embeddings_inference = TextEmbeddingsClient(TEI_SERVER)
    embeddings_inference = AsyncTextEmbeddingsClient(TEI_SERVER, producer)

    try:
        while True:
            # TODO has to use poll in order to work with DeserializingConsumer
            # TODO idea: make custom deserialization consumer
            consumer_start_time = perf_counter()
            messages = consumer.consume(num_messages=BATCH_SIZE, timeout=1)
            consumer_end_time = perf_counter()
            logger.info("Consumed %i messages from topic %s took %f seconds", len(messages), INPUT_TOPIC, consumer_end_time - consumer_start_time)
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

            for paper_chunk in paper_chunks:
                embeddings_inference.get_embedding(paper_chunk)
                # embeddings_inference.embed(paper_chunk.text_chunk)
                # embeddings_inference.process_paper(paper_chunk)
                # producer.produce_papers(paper_chunk)

            # inference_start_time = perf_counter()
            # inferred_papers = infer_embeddings(paper_chunks, embeddings_inference)
            # inference_end_time = perf_counter()
            # logger.info("Inferring chunks took %f seconds", inference_end_time - inference_start_time)
            #
            # group_metadata = consumer.consumer_group_metadata()
            # producer_start_time = perf_counter()
            # # futures = [producer.queue_papers(inferred) for inferred in inferred_papers]
            # producer.produce_papers(inferred_papers, get_offsets(messages), group_metadata)
            # producer_end_time = perf_counter()
            # logger.info("Producing chunks took %f seconds", producer_end_time - producer_start_time)
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


# def infer_embeddings(papers: list[Paper], embeddings_inference: TextEmbeddingsClient) -> list[Paper]:
#     abstract_list = [paper.text_chunk for paper in papers]
#     logger.info("Encoding embedding for %i paper chunks", len(papers))
#     # TODO are they in the same order? coming from Embedstream?
#     vectors = embeddings_inference.embed_batch(texts=abstract_list)
#     for i, vector in enumerate(vectors):
#         papers[i].embedding_vector = vector
#     return papers


# def get_offsets(messages):
#     offsets = []
#     for message in messages:
#         tp = TopicPartition(message.topic(), message.partition(), message.offset() + 1)
#         offsets.append(tp)
#     return offsets
