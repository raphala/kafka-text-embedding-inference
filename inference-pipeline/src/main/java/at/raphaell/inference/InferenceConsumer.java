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

public class InferenceConsumer<K, V> {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InferenceConsumer.class);
    private static final Duration POLL_DURATION = Duration.ofMillis(100);
    private final Consumer<K, V> consumer;
    private final Map<TopicPartition, OffsetAndMetadata> offsets;

    public InferenceConsumer(final Properties consumerProperties, final Deserializer<K> keyDeserializer,
            final Deserializer<V> valueDeserializer, final String inputTopic) {
        this.offsets = new ConcurrentHashMap<>();
        this.consumer = new KafkaConsumer<>(consumerProperties, keyDeserializer, valueDeserializer);
        log.info("Subscribing customer to topic {}", inputTopic);
        this.consumer.subscribe(List.of(inputTopic));
    }

    @VisibleForTesting
    public InferenceConsumer(final Consumer<K, V> consumer, final String inputTopic) {
        this.offsets = new ConcurrentHashMap<>();
        this.consumer = consumer;
        log.info("Subscribing customer to topic {}", inputTopic);
        this.consumer.subscribe(List.of(inputTopic));
    }

    public ConsumerRecords<K, V> poll() {
        return this.consumer.poll(POLL_DURATION);
    }

    public void addOffset(final TopicPartition topicPartition, final OffsetAndMetadata offsetAndMetadata) {
        this.offsets.put(topicPartition, offsetAndMetadata);
    }

    public void commit() {
        if (this.offsets.isEmpty()) {
            return;
        }
        this.consumer.commitSync(this.offsets);
        this.offsets.clear();
    }

    public void close() {
        this.consumer.close();
    }
}
