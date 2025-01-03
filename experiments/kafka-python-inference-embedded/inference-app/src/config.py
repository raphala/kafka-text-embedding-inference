import os

SCHEMA_REGISTRY = os.environ.get("SCHEMA_REGISTRY", "localhost:8081")
BOOTSTRAP_SERVER = os.environ.get("BOOTSTRAP_SERVER", "localhost:9092")
INPUT_TOPIC = os.environ.get("INPUT_TOPIC", "input-topic")
OUTPUT_TOPIC = os.environ.get("OUTPUT_TOPIC", "output-topic")
EXECUTION_PROVIDERS = os.environ.get("EXECUTION_PROVIDERS", "CUDAExecutionProvider,CPUExecutionProvider").split(",")
EMBEDDING_MODEL = os.environ.get("EMBEDDING_MODEL", "sentence-transformers/all-MiniLM-L6-v2")
CHUNK_SIZE = int(os.environ.get("CHUNK_SIZE", 512))
CHUNK_OVERLAP = int(os.environ.get("CHUNK_OVERLAP", 32))
BATCH_SIZE = int(os.environ.get("BATCH_SIZE", 32))
LOG_LEVEL = os.getenv('LOG_LEVEL', 'INFO')
