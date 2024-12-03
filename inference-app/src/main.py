import json
from pathlib import Path

from confluent_kafka.schema_registry import SchemaRegistryClient
from confluent_kafka.schema_registry.json_schema import JSONSerializer, JSONDeserializer
from confluent_kafka.serialization import StringDeserializer

import consumer
from config import SCHEMA_REGISTRY, BOOTSTRAP_SERVER, OUTPUT_TOPIC, BATCH_SIZE, CHUNK_SIZE, CHUNK_OVERLAP
from embeddingproducer import EmbeddingProducer
from logger import logger
from paper import dict_to_paper

# TODO exactly once - paper-producer and consumer
# TODO error handling - dead letter
# TODO prepare deployment
# TODO avro/protobuf support
# TODO autoscaling - scaling with transactional api?
# TODO implement deserializing consumer that supports everything

PROJECT_ROOT = Path(__file__).parent.parent.parent


def load_schema(schema_name: str) -> dict:
    schema_path = PROJECT_ROOT / "schemas" / schema_name
    try:
        with open(schema_path) as f:
            return json.load(f)
    except FileNotFoundError:
        raise FileNotFoundError(f"Schema file not found at {schema_path}")


if __name__ == '__main__':
    logger.info("Starting inference app with provider %s, chunk size %i, chunk overlap %i, batch size %i",  CHUNK_SIZE, CHUNK_OVERLAP, BATCH_SIZE)
    schema_registry_conf = {'url': SCHEMA_REGISTRY}
    schema_registry_client = SchemaRegistryClient(schema_registry_conf)
    string_deserializer = StringDeserializer('utf_8')

    paper_schema = load_schema("paper.json")
    paper_schema_str = json.dumps(paper_schema)
    paper_json_deserializer = JSONDeserializer(schema_str=paper_schema_str,
                                               schema_registry_client=schema_registry_client, from_dict=dict_to_paper)

    qdrant_schema = load_schema("qdrant-sink-message.json")
    qdrant_schema_str = json.dumps(qdrant_schema)
    qdrant_json_serializer = JSONSerializer(schema_str=qdrant_schema_str, schema_registry_client=schema_registry_client)

    consumer_config = {
        'bootstrap.servers': BOOTSTRAP_SERVER,
        'group.id': 'embeddings',
        'auto.offset.reset': 'earliest',
        'isolation.level': 'read_committed',
        'enable.auto.commit': False
    }

    producer_config = {
        'bootstrap.servers': BOOTSTRAP_SERVER,
        'transactional.id': 'embeddings-producer-1',
    }

    producer = EmbeddingProducer(producer_config, qdrant_json_serializer, OUTPUT_TOPIC)
    consumer.run_consumer(consumer_config, producer, paper_json_deserializer)
