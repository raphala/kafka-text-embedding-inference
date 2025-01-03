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

public class InferenceProducer<Key, InputValue, OutputValue> {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InferenceProducer.class);
    private final Producer<Key, OutputValue> producer;
    private final InferenceConsumer<Key, InputValue> inferenceConsumer;

    public InferenceProducer(final Properties producerProperties, final Serializer<Key> keySerializer,
            final Serializer<OutputValue> valueSerializer, final InferenceConsumer<Key, InputValue> inferenceConsumer) {
        this.inferenceConsumer = inferenceConsumer;
        this.producer = new KafkaProducer<>(producerProperties, keySerializer, valueSerializer);
    }

    @VisibleForTesting
    public InferenceProducer(final Producer<Key, OutputValue> producer,
            final InferenceConsumer<Key, InputValue> inferenceConsumer) {
        this.inferenceConsumer = inferenceConsumer;
        this.producer = producer;
    }

    public void send(final ProducerRecord<Key, OutputValue> producerRecord) {
        log.info("Sending embedded message to topic {}", producerRecord.topic());
        this.producer.send(producerRecord, this::sendCallback);
    }

    public void close() {
        this.producer.close();
    }

    private void sendCallback(final RecordMetadata metadata, final Exception exception) {
        if (exception != null) {
            log.error("Message could not be produced", exception);
        } else {
            final TopicPartition topicPartition = new TopicPartition(metadata.topic(), metadata.partition());
            final OffsetAndMetadata offsetAndMetadata = new OffsetAndMetadata(metadata.offset() + 1);
            this.inferenceConsumer.addOffset(topicPartition, offsetAndMetadata);
        }
    }

}
