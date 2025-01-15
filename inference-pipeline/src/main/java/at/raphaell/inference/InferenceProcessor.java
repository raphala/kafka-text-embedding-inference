package at.raphaell.inference;

import at.raphaell.inference.models.Chunkable;
import at.raphaell.inference.models.ChunkedChunkable;
import at.raphaell.inference.models.EmbeddedChunkable;
import java.util.ArrayList;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;

/**
 * Processes incoming Kafka records, chunks the text, embeds each chunk, and sends the results to an output topic.
 *
 * @param <Key> type of keys used for consumed and produced messages
 * @param <InputValue> type of input values consumed, must extend {@link Chunkable}
 * @param <OutputValue> the type of output values produced
 */
public class InferenceProcessor<Key, InputValue extends Chunkable, OutputValue> {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InferenceProcessor.class);
    private final InferenceApp<Key, InputValue, OutputValue> inferenceApp;
    private final List<Thread> threads;

    /**
     * Constructs an {@code InferenceProcessor}. The specified {@link InferenceApp} is used to process records.
     */
    public InferenceProcessor(final InferenceApp<Key, InputValue, OutputValue> inferenceApp) {
        this.inferenceApp = inferenceApp;
        this.threads = new ArrayList<>();
    }

    /**
     * Polls records from the Kafka consumer and processes them. If no records are polled, the method returns
     * immediately.
     */
    public void pollRecords() {
        final ConsumerRecords<Key, InputValue> consumerRecords = this.inferenceApp.getInferenceConsumer().poll();
        if (consumerRecords.isEmpty()) {
            return;
        }
        log.info("Polled {} records", consumerRecords.count());
        consumerRecords.forEach(this::processRecord);
        // wait for all chunks to be fully processed
        this.joinThreads();
        // commit offsets after current batch is successfully processed
        this.inferenceApp.getInferenceConsumer().commit();
    }

    /**
     * Processes a single Kafka consumer record by chunking it and handling each chunk in a virtual thread.
     */
    private void processRecord(final ConsumerRecord<Key, InputValue> consumerRecord) {
        final List<ChunkedChunkable> chunks = this.inferenceApp.getChunker().chunkText(consumerRecord.value());
        log.info("Processing record split into {} chunks", chunks.size());
        for (final ChunkedChunkable chunkedChunkable : chunks) {
            final Thread thread = Thread.startVirtualThread(() -> {
                log.info("Sending EmbedRequest to API in new thread");
                this.sendEmbedRequest(chunkedChunkable, consumerRecord);
            });
            this.threads.add(thread);
        }
    }

    /**
     * Sends an embedding request for a chunked text and produces the result to the Kafka output topic.
     */
    private void sendEmbedRequest(final ChunkedChunkable chunkedChunkable,
            final ConsumerRecord<Key, InputValue> consumerRecord) {
        try {
            final List<Float> embedding = this.inferenceApp.getEmbedClient().embed(chunkedChunkable);
            final EmbeddedChunkable embeddedChunkable = new EmbeddedChunkable(chunkedChunkable, embedding);
            final OutputValue outputValue = this.inferenceApp.transformToOutputMessage(embeddedChunkable);
            final ProducerRecord<Key, OutputValue> producerRecord =
                    new ProducerRecord<>(this.inferenceApp.getInferenceArgs().getOutputTopic(),
                            consumerRecord.key(),
                            outputValue);
            this.inferenceApp.getInferenceProducer().send(producerRecord);
        } catch (final RuntimeException exception) {
            log.error("Could not embed and produce ConsumerRecord", exception);
        }
    }

    /**
     * Waits for all spawned threads to complete their execution and clears the thread list.
     */
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

}
