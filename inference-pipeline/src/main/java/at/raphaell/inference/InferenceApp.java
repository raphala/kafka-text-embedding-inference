package at.raphaell.inference;

import at.raphaell.inference.chunking.Chunker;
import at.raphaell.inference.models.Chunkable;
import at.raphaell.inference.models.ChunkedChunkable;
import at.raphaell.inference.models.EmbeddedChunkable;
import com.google.common.annotations.VisibleForTesting;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command
public abstract class InferenceApp<Key, InputValue extends Chunkable, OutputValue> implements Runnable {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InferenceApp.class);
    private static final int TERMINATION_TIMEOUT = 60;
    private final List<Thread> threads;
    private final SerializationConfig serializationConfig;
    protected Properties consumerProperties = null;
    protected Properties producerProperties = null;
    protected InferenceConsumer<Key, InputValue> inferenceConsumer = null;
    protected InferenceProducer<Key, InputValue, OutputValue> inferenceProducer = null;
    protected Chunker chunker = null;
    protected ExecutorService executorService = null;
    protected EmbedClient embedClient = null;
    @Mixin
    protected InferenceArgs inferenceArgs;
    private volatile boolean running = true;

    protected InferenceApp(final SerializationConfig serializationConfig) {
        this.serializationConfig = serializationConfig;
        this.threads = new ArrayList<>();
    }

    @Override
    public void run() {
        this.producerProperties = this.createProducerProperties();
        this.consumerProperties = this.createConsumerProperties();
        this.initApp();
        this.start();
    }

    public abstract OutputValue transformMessage(EmbeddedChunkable embeddedChunkable);

    public abstract Chunker createChunker();

    public abstract String getGroupId();

    public abstract Deserializer<Key> getKeyDeserializer();

    public abstract Deserializer<InputValue> getInputValueDeserializer();

    public abstract Serializer<Key> getKeySerializer();

    public abstract Serializer<OutputValue> getOutputValueSerializer();

    protected Properties createProducerProperties() {
        final Properties properties = new Properties();
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                this.serializationConfig.keySerializerClass().getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                this.serializationConfig.valueSerializerClass().getName());
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.getInferenceArgs().getBootstrapServer());
        properties.setProperty(ProducerConfig.ACKS_CONFIG, "all");
        properties.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip");
        if (this.getInferenceArgs().getSchemaRegistry() != null) {
            properties.setProperty("schema.registry.url", this.getInferenceArgs().getSchemaRegistry());
        }
        return properties;
    }

    protected Properties createConsumerProperties() {
        final Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                this.serializationConfig.keySerializerClass().getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                this.serializationConfig.valueDeserializerClass().getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, this.getGroupId());
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.getInferenceArgs().getBootstrapServer());
        properties.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, this.getInferenceArgs().getBatchSize());
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        if (this.getInferenceArgs().getSchemaRegistry() != null) {
            properties.setProperty("schema.registry.url", this.getInferenceArgs().getSchemaRegistry());
        }
        return properties;
    }

    @VisibleForTesting
    protected void initApp() {
        this.inferenceConsumer =
                new InferenceConsumer<>(this.consumerProperties, this.getKeyDeserializer(),
                        this.getInputValueDeserializer(), this.inferenceArgs.getInputTopic());
        this.inferenceProducer =
                new InferenceProducer<>(this.producerProperties, this.getKeySerializer(),
                        this.getOutputValueSerializer(), this.inferenceConsumer);
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.embedClient = new EmbedClient(
                ManagedChannelBuilder.forAddress(this.inferenceArgs.getTeiHost(), this.inferenceArgs.getTeiPort())
                        .usePlaintext()
                        .build());
        this.chunker = this.createChunker();
    }

    private InferenceArgs getInferenceArgs() {
        return this.inferenceArgs;
    }

    private void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        while (this.running) {
            final ConsumerRecords<Key, InputValue> consumerRecords = this.inferenceConsumer.poll();
            if (consumerRecords.count() == 0) {
                continue;
            }
            log.info("Polled {} records", consumerRecords.count());
            consumerRecords.forEach(this::processRecord);
            this.joinThreads();
            this.inferenceConsumer.commit();
        }
    }

    private void processRecord(final ConsumerRecord<Key, InputValue> consumerRecord) {
        final List<ChunkedChunkable> chunks = this.chunker.chunkText(consumerRecord.value());
        log.info("Processing record split into {} chunks", chunks.size());
        for (final ChunkedChunkable chunkedChunkable : chunks) {
            final Thread thread = Thread.startVirtualThread(() -> {
                try {
                    log.info("Sending EmbedRequest to API in new thread");
                    final List<Float> embedding = this.embedClient.embed(chunkedChunkable);
                    final EmbeddedChunkable embeddedChunkable = new EmbeddedChunkable(chunkedChunkable, embedding);
                    final OutputValue outputValue = this.transformMessage(embeddedChunkable);
                    final ProducerRecord<Key, OutputValue> producerRecord =
                            new ProducerRecord<>(this.inferenceArgs.getOutputTopic(), consumerRecord.key(),
                                    outputValue);
                    this.inferenceProducer.send(producerRecord);
                } catch (final RuntimeException exception) {
                    log.error("Could not embed and produce ConsumerRecord", exception);
                }
            });
            this.threads.add(thread);
        }
    }

    private void joinThreads() {
        this.threads.forEach(thread -> {
            try {
                thread.join();
            } catch (final InterruptedException exception) {
                Thread.currentThread().interrupt();
                log.error("Main thread interrupted", exception);
            }
        });
        this.threads.clear();
    }

    private void shutdown() {
        this.running = false;
        try {
            this.executorService.shutdown();
            if (!this.executorService.awaitTermination(TERMINATION_TIMEOUT, TimeUnit.SECONDS)) {
                this.executorService.shutdownNow();
            }
        } catch (final InterruptedException exception) {
            this.executorService.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            this.inferenceConsumer.close();
            this.inferenceProducer.close();
        }
    }
}
