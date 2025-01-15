package at.raphaell.inference;

import com.google.common.annotations.VisibleForTesting;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.Deserializer;

/**
 * Kafka consumer for processing incoming messages for inference.
 *
 * @param <K> type of keys consumed
 * @param <V> type of values consumed
 */
public class InferenceConsumer<K, V> {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InferenceConsumer.class);
    private static final Duration POLL_DURATION = Duration.ofMillis(100);
    private final Consumer<K, V> consumer;
    private final Map<TopicPartition, OffsetAndMetadata> offsets;

    /**
     * Constructs an {@code InferenceConsumer} with the specified properties and deserializers.
     *
     * @param consumerProperties properties for the consumer
     * @param keyDeserializer deserializer for keys
     * @param valueDeserializer deserializer for values
     * @param inputTopic input topic to subscribe to
     */
    public InferenceConsumer(final Properties consumerProperties, final Deserializer<K> keyDeserializer,
            final Deserializer<V> valueDeserializer, final String inputTopic) {
        this.offsets = new ConcurrentHashMap<>();
        this.consumer = new KafkaConsumer<>(consumerProperties, keyDeserializer, valueDeserializer);
        log.info("Subscribing customer to topic {}", inputTopic);
        this.consumer.subscribe(List.of(inputTopic));
    }

    /**
     * Constructs an {@code InferenceConsumer} for testing purposes. This is intended for testing purposes only.
     *
     * @param consumer Kafka consumer instance
     * @param inputTopic input topic to subscribe to
     */
    @VisibleForTesting
    public InferenceConsumer(final Consumer<K, V> consumer, final String inputTopic) {
        this.offsets = new ConcurrentHashMap<>();
        this.consumer = consumer;
        log.info("Subscribing customer to topic {}", inputTopic);
        this.consumer.subscribe(List.of(inputTopic));
    }

    /**
     * Polls the consumer for new records.
     *
     * @return consumer records polled
     */
    public ConsumerRecords<K, V> poll() {
        return this.consumer.poll(POLL_DURATION);
    }

    /**
     * Adds an offset to the commit map. This offset will be committed when calling {@link #commit()}.
     *
     * @param topicPartition topic partition
     * @param offsetAndMetadata offset and metadata
     */
    public void addOffset(final TopicPartition topicPartition, final OffsetAndMetadata offsetAndMetadata) {
        this.offsets.put(topicPartition, offsetAndMetadata);
    }

    /**
     * Commits the offsets that have been previously added using {@link #addOffset(TopicPartition, OffsetAndMetadata)}.
     */
    public void commit() {
        if (this.offsets.isEmpty()) {
            return;
        }
        this.consumer.commitSync(this.offsets);
        this.offsets.clear();
    }

    /**
     * Closes the consumer.
     */
    public void close() {
        this.consumer.close();
    }
}
