package com.bakdata.inference;

import com.bakdata.inference.Chunker.ChunkWithPaper;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializer;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer;
import io.grpc.ManagedChannelBuilder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import picocli.CommandLine.Option;

public class InferenceApp {

//    @Option(names = "--host", description = "the hostname of the grpc inference service", required = true)
//    private String host;
//    @Option(names = "--port", description = "the port of the grpc inference service", required = true)
//    private int port;
//    @Option(names = "--chunk-size", description = "the size of individual chunks to embed", required = true)
//    private int chunkSize;
//    @Option(names = "--chunk-overlap", description = "the overlap between chunks", required = true)
//    private int chunkOverlap;
//    @Option(names = "--input-topic", description = "the topic to consume paper from", required = true)
    private String inputTopic = System.getenv("INPUT_TOPIC");
//    @Option(names = "--output-topic", description = "the topic to produce embeddings to", required = true)
    private String outputTopic = System.getenv("OUTPUT_TOPIC");

    private final KafkaConsumer<String, Paper> consumer;
    private final KafkaProducer<String, EmbeddedPaper> producer;
    private final EmbedClient embedClient;
    private final Chunker chunker;
    private final ExecutorService executorService;
    private volatile boolean running = true;

    public static final String COLLECTION_NAME = "embedding";
//     TODO extract vars

    private InferenceApp() {
        this.embedClient = new EmbedClient(ManagedChannelBuilder.forAddress("text-embeddings.personal-raphael-lachtner.svc.cluster.local", 50051)
                .usePlaintext()
                .build());
        this.chunker = new Chunker(1000, 50);
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        final Properties consumerProps = new Properties();
        consumerProps.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "strimzi-k8kafka-kafka-bootstrap.infrastructure.svc:9092");
        consumerProps.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "kafka-api-inference");
        consumerProps.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaJsonSchemaDeserializer.class.getName());
        consumerProps.setProperty("schema.registry.url", "http://k8kafka-cp-schema-registry.infrastructure.svc.cluster.local:8081");
        consumerProps.setProperty("json.value.type", Paper.class.getName());
        consumerProps.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "1");
//        consumerProps.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        consumerProps.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        this.consumer = new KafkaConsumer<>(consumerProps, new StringDeserializer(), SerdeUtils.getSerde(Paper.class, (Map)consumerProps).deserializer());

        final Properties producerProps = new Properties();
        producerProps.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "strimzi-k8kafka-kafka-bootstrap.infrastructure.svc:9092");
        producerProps.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaJsonSchemaSerializer.class.getName());
        producerProps.setProperty(ProducerConfig.ACKS_CONFIG, "all");
        producerProps.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip");
        producerProps.setProperty("schema.registry.url", "http://k8kafka-cp-schema-registry.infrastructure.svc.cluster.local:8081");
        producerProps.setProperty("json.value.type", Paper.class.getName());
//        producerProps.setProperty(ProducerConfig.RETRIES_CONFIG, 3);
//        producerProps.setProperty(ProducerConfig.LINGER_MS_CONFIG, 1);
//        producerProps.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        this.producer = new KafkaProducer<>(producerProps, new StringSerializer(), SerdeUtils.getSerde(Paper.class, (Map)consumerProps).serializer());
    }

    public static void main(final String[] args) {
        final InferenceApp pipeline = new InferenceApp();

        Runtime.getRuntime().addShutdownHook(new Thread(pipeline::shutdown));
        pipeline.start();
    }

    public void start() {
        this.consumer.subscribe(List.of(this.inputTopic));

        while (this.running) {
            final ConsumerRecords<String, Paper> records = this.consumer.poll(Duration.ofMillis(100));
            if (records.isEmpty()) {
                continue;
            }

            // Process batches in parallel using CompletableFuture
            final List<CompletableFuture<Void>> futures = new ArrayList<>();
            final Map<TopicPartition, OffsetAndMetadata> offsetsToCommit = new ConcurrentHashMap<>();

            for (final ConsumerRecord<String, Paper> record : records) {
                final List<ChunkWithPaper> chunks = this.chunker.createChunks(record.value());
                for (final ChunkWithPaper chunk: chunks) {
                    final CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        try {
                            // Process record through API
                            final EmbeddedPaper embeddedPaper = EmbeddedPaper.fromPaper(chunk,
                                                this.embedClient.embed(chunk.chunk()),
                                                UUID.randomUUID().toString());

                            // Produce result to target topic
                            final ProducerRecord<String, EmbeddedPaper> producerRecord = new ProducerRecord<>(this.outputTopic, record.key(), embeddedPaper);

                            this.producer.send(producerRecord, (metadata, exception) -> {
                                if (exception != null) {
                                    exception.printStackTrace();
                                } else {
                                    // Update offsets after successful production
                                    offsetsToCommit.put(
                                            new TopicPartition(record.topic(), record.partition()),
                                            new OffsetAndMetadata(record.offset() + 1)
                                    );
                                }
                            });
                        } catch (final Exception e) {
                            e.printStackTrace();
                        }
                    }, this.executorService);

                    futures.add(future);
                }
            }

            // Wait for all processing to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // Commit offsets for successfully processed records
            if (!offsetsToCommit.isEmpty()) {
                this.consumer.commitSync(offsetsToCommit);
            }
        }
    }

    public void shutdown() {
        this.running = false;
        try {
            this.executorService.shutdown();
            if (!this.executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                this.executorService.shutdownNow();
            }
        } catch (final InterruptedException e) {
            this.executorService.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            this.consumer.close();
            this.producer.close();
        }
    }

//    // Mock external API service
//    private static class ExternalApiService {
//        public EmbeddedPaper processData(final Paper data) {
//            // Simulate some processing time
//            try {
//                Thread.sleep(100);
//            } catch (final InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//            return null;
//        }
//    }
}
