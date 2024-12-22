# Kafka Inference Pipeline

A streamlined Java library for building Kafka streaming pipelines that generated text embeddings using the highly optimized  
[huggingface/text-embeddings-inference (TEI)](https://github.com/huggingface/text-embeddings-inference)
service. 

The library takes care of the whole embedding process:

- Consume messages from Kafka
- Batch messages for optimal processing
- Split messages into chunks
- Generate embeddings using the TEI gRPC API
- Produce enriched messages back to Kafka

#### paper-inference-app

An example implementation that demonstrates how to use the library to:
- Process academic papers from Kafka
- Naively chunk paper abstracts for embedding generation
- Produce embedded papers to Kafka in a format compatible with Qdrant sink connector

### Features

- **Efficient Message Batching**: Optimizes throughput when embedding messages from Kafka
- **Configurable Pipeline**: Easy setup with CLI options for all necessary configurations
- **Customizable Chunking**: Flexible chunking stage
- **Message Serialization**: Support for different serialization formats
- **TEI Service Integration**: Direct integration with huggingface/text-embeddings-inference via gRPC

### Prerequisites

- Running Kafka cluster with topics for input and output
- Running huggingface/text-embeddings-inference service

### Configuration

```properties
--batch-size         # Size of message batches (default: 1)
--bootstrap-server   # Kafka bootstrap server address
--schema-registry    # Schema registry URL
--input-topic       # Source Kafka topic
--output-topic      # Destination Kafka topic
--tei-host          # Text Embeddings Inference service host
--tei-port          # Text Embeddings Inference service port (default: 50051)
```
