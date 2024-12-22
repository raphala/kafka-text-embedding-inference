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
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
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
    private Properties consumerProperties = null;
    private Properties producerProperties = null;
    private InferenceConsumer<Key, InputValue> inferenceConsumer = null;
    private InferenceProducer<Key, InputValue, OutputValue> inferenceProducer = null;
    private Chunker chunker = null;
    private ExecutorService executorService = null;
    private EmbedClient embedClient = null;
    private volatile boolean running = true;

    @Mixin
    private InferenceArgs inferenceArgs;

    protected InferenceApp() {
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

    public abstract Properties createConsumerProperties();

    public abstract Properties createProducerProperties();

    public abstract Deserializer<Key> getKeyDeserializer();

    public abstract Deserializer<InputValue> getInputValueDeserializer();

    public abstract Serializer<Key> getKeySerializer();

    public abstract Serializer<OutputValue> getOutputValueSerializer();

    protected InferenceArgs getInferenceArgs() {
        return this.inferenceArgs;
    }

    private void initApp() {
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
