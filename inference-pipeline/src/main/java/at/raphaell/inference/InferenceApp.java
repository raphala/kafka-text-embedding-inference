package at.raphaell.inference;

import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

public class InferenceApp<Key, InputValue extends Chunkable, OutputValue> {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InferenceApp.class);
    private static final int TERMINATION_TIMEOUT = 60;
    private volatile boolean running = true;
    private InferenceConsumer<Key, InputValue> inferenceConsumer;
    private InferenceProducer<Key, InputValue, OutputValue> inferenceProducer;
    private Chunker chunker;
    private ExecutorService executorService;
    private EmbedClient embedClient;
    private Function<EmbeddedChunkable, OutputValue> function;
    private List<Thread> threads;

    public Properties getProducerProperties() {
        return new Properties();
    }

    public Properties getConsumerProperties() {
        return new Properties();
    }

    public void start() {
        while (this.running) {
            final ConsumerRecords<Key, InputValue> consumerRecords = this.inferenceConsumer.poll();
            consumerRecords.forEach(this::processRecord);
            this.joinThreads();
            this.inferenceConsumer.commit();
        }
    }

    protected void InferenceApp(final Deserializer<Key> keyDeserializer,
            final Deserializer<InputValue> valueDeserializer,
            final Serializer<Key> keySerializer,
            final Serializer<OutputValue> valueSerializer,
            final Chunker chunker) {
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.threads = new ArrayList<>();
        this.chunker = chunker;
        this.embedClient = new EmbedClient(
                ManagedChannelBuilder.forAddress(InferenceConfig.TEI_HOST, InferenceConfig.TEI_PORT)
                        .usePlaintext()
                        .build());
        this.inferenceConsumer =
                new InferenceConsumer<>(this.getConsumerProperties(), keyDeserializer, valueDeserializer);
        this.inferenceProducer = new InferenceProducer<>(this.getProducerProperties(), keySerializer, valueSerializer,
                this.inferenceConsumer);
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        this.start();
    }

    private void processRecord(final ConsumerRecord<Key, InputValue> consumerRecord) {
        final List<ChunkedChunkable> chunks = this.chunker.chunkText(consumerRecord.value());
        for (final ChunkedChunkable chunkedChunkable : chunks) {
            final Thread thread = Thread.startVirtualThread(() -> {
                try {
                    final List<Float> embedding = this.embedClient.embed(chunkedChunkable);
                    final EmbeddedChunkable embeddedChunkable = new EmbeddedChunkable(chunkedChunkable, embedding);
                    final OutputValue outputValue = this.function.apply(embeddedChunkable);
                    final ProducerRecord<Key, OutputValue> producerRecord =
                            new ProducerRecord<>(InferenceConfig.OUTPUT_TOPIC, consumerRecord.key(), outputValue);
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
