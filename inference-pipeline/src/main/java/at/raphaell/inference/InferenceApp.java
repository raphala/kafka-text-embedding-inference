package at.raphaell.inference;

import at.raphaell.inference.chunking.Chunker;
import at.raphaell.inference.models.Chunkable;
import at.raphaell.inference.models.EmbeddedChunkable;
import com.google.common.annotations.VisibleForTesting;
import io.grpc.ManagedChannelBuilder;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

/**
 * Abstract base class for inference applications.
 *
 * @param <Key> type of keys used for consumed and produced messages
 * @param <InputValue> type of input values consumed, must extend {@link Chunkable}
 * @param <OutputValue> type of output values produced
 */
@Command
public abstract class InferenceApp<Key, InputValue extends Chunkable, OutputValue> implements Runnable, AutoCloseable {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InferenceApp.class);
    private final SerializationConfig serializationConfig;
    protected Properties consumerProperties = null;
    protected Properties producerProperties = null;
    protected InferenceConsumer<Key, InputValue> inferenceConsumer = null;
    protected InferenceProducer<Key, InputValue, OutputValue> inferenceProducer = null;
    protected Chunker chunker = null;
    protected EmbedClient embedClient = null;
    protected InferenceProcessor<Key, InputValue, OutputValue> inferenceProcessor = null;
    @Mixin
    private InferenceArgs inferenceArgs;
    private volatile boolean running = true;

    /**
     * Constructs an {@code InferenceApp} with the specified serialization configuration.
     *
     * @param serializationConfig serialization configuration
     */
    protected InferenceApp(final SerializationConfig serializationConfig) {
        this.serializationConfig = serializationConfig;
    }

    @Override
    public void run() {
        // the command-line arguments have been parsed by picocli at this point
        this.producerProperties = this.createProducerProperties();
        log.info("Initializing app with producer properties: {}", this.producerProperties);
        this.consumerProperties = this.createConsumerProperties();
        log.info("Initializing app with consumer properties: {}", this.consumerProperties);
        this.initApp();
        this.start();
    }

    /**
     * Transforms a chunked and embedded input value {@link EmbeddedChunkable} into the output message type.
     *
     * @param embeddedChunkable embedded chunkable to transform
     * @return transformed output value
     */
    public abstract OutputValue transformToOutputMessage(EmbeddedChunkable embeddedChunkable);

    /**
     * Creates a {@link Chunker} for chunking consumed messages.
     */
    public abstract Chunker createChunker();

    /**
     * Returns the group ID for the Kafka consumer group.
     */
    public abstract String getGroupId();

    /**
     * Provides the deserializer for message keys.
     */
    public abstract Deserializer<Key> getKeyDeserializer();

    /**
     * Provides the deserializer for input values.
     */
    public abstract Deserializer<InputValue> getInputValueDeserializer();

    /**
     * Provides the serializer for message keys.
     */
    public abstract Serializer<Key> getKeySerializer();

    /**
     * Provides the serializer for output values.
     */
    public abstract Serializer<OutputValue> getOutputValueSerializer();

    public InferenceConsumer<Key, InputValue> getInferenceConsumer() {
        return this.inferenceConsumer;
    }

    public InferenceProducer<Key, InputValue, OutputValue> getInferenceProducer() {
        return this.inferenceProducer;
    }

    public Chunker getChunker() {
        return this.chunker;
    }

    public EmbedClient getEmbedClient() {
        return this.embedClient;
    }

    public InferenceArgs getInferenceArgs() {
        return this.inferenceArgs;
    }

    /**
     * Shuts down the application gracefully by closing the Kafka consumer and producer.
     */
    @Override
    public void close() {
        this.running = false;
        this.inferenceConsumer.close();
        this.inferenceProducer.close();
    }

    /**
     * Creates common producer properties for the Kafka producer and might be overwritten, if necessary. Must be called
     * after command-line arguments have been parsed and fields are populated.
     */
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

    /**
     * Creates common consumer properties for the Kafka consumer and might be overwritten, if necessary. Must be called
     * after command-line arguments have been parsed and fields are populated.
     */
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

    /**
     * Initializes the application components. Increased visibility for testing purposes.
     */
    @VisibleForTesting
    protected void initApp() {
        this.inferenceConsumer =
                new InferenceConsumer<>(this.consumerProperties, this.getKeyDeserializer(),
                        this.getInputValueDeserializer(), this.inferenceArgs.getInputTopic());
        this.inferenceProducer =
                new InferenceProducer<>(this.producerProperties, this.getKeySerializer(),
                        this.getOutputValueSerializer(), this.inferenceConsumer);
        this.embedClient = new EmbedClient(
                ManagedChannelBuilder.forAddress(this.inferenceArgs.getTeiHost(), this.inferenceArgs.getTeiPort())
                        .usePlaintext()
                        .build());
        this.chunker = this.createChunker();
        this.inferenceProcessor = new InferenceProcessor<>(this);
    }

    private void start() {
        log.info("Starting inference app");
        // add shutdown hook to gracefully shut down the application
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));

        // start the main message processing loop
        while (this.running) {
            this.inferenceProcessor.pollRecords();
        }
    }
}
