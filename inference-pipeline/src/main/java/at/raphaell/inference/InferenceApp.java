package at.raphaell.inference;

import at.raphaell.inference.chunking.Chunker;
import at.raphaell.inference.models.Chunkable;
import at.raphaell.inference.models.ChunkedChunkable;
import at.raphaell.inference.models.EmbeddedChunkable;
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

public abstract class InferenceApp<Key, InputValue extends Chunkable, OutputValue> {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InferenceApp.class);
    private static final int TERMINATION_TIMEOUT = 60;
    private final Properties consumerProperties;
    private final Properties producerProperties;
    private final InferenceConsumer<Key, InputValue> inferenceConsumer;
    private final InferenceProducer<Key, InputValue, OutputValue> inferenceProducer;
    private final Chunker chunker;
    private final ExecutorService executorService;
    private final EmbedClient embedClient;
    private final List<Thread> threads;
    private volatile boolean running = true;

    protected InferenceApp(final InferenceConfig<Key, InputValue, OutputValue> inferenceConfig) {
        this.consumerProperties = createBaseConsumerProperties();
        this.producerProperties = createBaseProducerProperties();
        this.inferenceConsumer =
                new InferenceConsumer<>(this.consumerProperties, inferenceConfig.keyDeserializer(),
                        inferenceConfig.valueDeserializer());
        this.inferenceProducer =
                new InferenceProducer<>(this.producerProperties, inferenceConfig.keySerializer(),
                        inferenceConfig.valueSerializer(), this.inferenceConsumer);
        this.chunker = inferenceConfig.chunker();
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.embedClient = new EmbedClient(
                ManagedChannelBuilder.forAddress(InferenceVar.TEI_HOST, InferenceVar.TEI_PORT)
                        .usePlaintext()
                        .build());
        this.threads = new ArrayList<>();
    }

    public static Properties createBaseProducerProperties() {
        final Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, InferenceVar.BOOTSTRAP_SERVER);
        properties.setProperty(ProducerConfig.ACKS_CONFIG, "all");
        properties.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip");
        properties.setProperty("schema.registry.url", InferenceVar.SCHEMA_REGISTRY);
        return properties;
    }

    public static Properties createBaseConsumerProperties() {
        final Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, InferenceVar.BOOTSTRAP_SERVER);
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, InferenceVar.GROUP_ID);
        properties.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, InferenceVar.BATCH_SIZE);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.setProperty("schema.registry.url", InferenceVar.SCHEMA_REGISTRY);
        return properties;
    }

    public abstract OutputValue transformMessage(EmbeddedChunkable embeddedChunkable);

    protected void start() {
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
                            new ProducerRecord<>(InferenceVar.OUTPUT_TOPIC, consumerRecord.key(), outputValue);
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
