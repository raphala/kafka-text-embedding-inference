package at.raphaell.inference;

import com.google.common.annotations.VisibleForTesting;
import java.util.Properties;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.Serializer;

/**
 * Kafka producer for sending messages after inference processing.
 *
 * @param <Key> type of keys produced
 * @param <InputValue> type of the original input values
 * @param <OutputValue> type of output values produced
 */
public class InferenceProducer<Key, InputValue, OutputValue> {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InferenceProducer.class);
    private final Producer<Key, OutputValue> producer;
    private final InferenceConsumer<Key, InputValue> inferenceConsumer;

    /**
     * Constructs an {@code InferenceProducer} with the specified properties and serializers.
     *
     * @param producerProperties properties for the producer
     * @param keySerializer serializer for keys
     * @param valueSerializer serializer for values
     * @param inferenceConsumer consumer associated with this producer for offset management
     */
    public InferenceProducer(final Properties producerProperties, final Serializer<Key> keySerializer,
            final Serializer<OutputValue> valueSerializer, final InferenceConsumer<Key, InputValue> inferenceConsumer) {
        this.inferenceConsumer = inferenceConsumer;
        this.producer = new KafkaProducer<>(producerProperties, keySerializer, valueSerializer);
    }

    /**
     * Constructs an {@code InferenceProducer} for testing purposes. This is intended for testing purposes only.
     *
     * @param producer Kafka producer instance
     * @param inferenceConsumer consumer associated with this producer
     */
    @VisibleForTesting
    public InferenceProducer(final Producer<Key, OutputValue> producer,
            final InferenceConsumer<Key, InputValue> inferenceConsumer) {
        this.inferenceConsumer = inferenceConsumer;
        this.producer = producer;
    }

    /**
     * Sends a record to the Kafka topic.
     *
     * @param producerRecord record to send
     */
    public void send(final ProducerRecord<Key, OutputValue> producerRecord) {
        log.info("Sending embedded message to topic {}", producerRecord.topic());
        this.producer.send(producerRecord, this::sendCallback);
    }

    /**
     * Closes the producer.
     */
    public void close() {
        this.producer.close();
    }

    private void sendCallback(final RecordMetadata metadata, final Exception exception) {
        // this is used to check if the message was produced and acknowledged successfully
        if (exception != null) {
            // for now, we just log the error, in the future we might want to create dead letters
            log.error("Message could not be produced", exception);
        }
        final TopicPartition topicPartition = new TopicPartition(metadata.topic(), metadata.partition());
        final OffsetAndMetadata offsetAndMetadata = new OffsetAndMetadata(metadata.offset() + 1);
        // if the message was produced successfully, the offset is added to the commit map
        this.inferenceConsumer.addOffset(topicPartition, offsetAndMetadata);
    }

}
