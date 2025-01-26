package at.raphaell.inference;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InferenceProducerTest {

    private MockProducer<String, String> mockProducer;
    private MockConsumer<String, String> mockConsumer;
    private Properties producerProperties;
    private InferenceProducer<String, String, String> inferenceProducer;
    private InferenceConsumer<String, String> inferenceConsumer;

    @BeforeEach
    void setup() {
        // Initialize mock producer and consumer
        this.mockProducer = new MockProducer<>(true, new StringSerializer(), new StringSerializer());
        this.mockConsumer = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
        this.producerProperties = new Properties();
        this.producerProperties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        this.producerProperties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        this.producerProperties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "mock://kafka");
        this.producerProperties.setProperty(ProducerConfig.ACKS_CONFIG, "all");
        this.producerProperties.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip");
        this.inferenceConsumer = new InferenceConsumer<>(this.mockConsumer, "input");
        this.inferenceProducer = new InferenceProducer<>(this.mockProducer, this.inferenceConsumer);
    }

    @Test
    void shouldSendProducerRecord() {
        final ProducerRecord<String, String> producerRecord = new ProducerRecord<>("output", "key", "value");
        this.inferenceProducer.send(producerRecord);
        final var history = this.mockProducer.history();
        assertThat(history).hasSize(1);
        final ProducerRecord<String, String> record = history.get(0);
        assertThat(record).isEqualTo(producerRecord);
    }

    @Test
    void shouldCommitOffsets() {
        final TopicPartition topicPartition = new TopicPartition("output", 0);
        final ProducerRecord<String, String> producerRecord = new ProducerRecord<>("output", 0, "key", "value");
        this.mockConsumer.rebalance(List.of(topicPartition));
        this.mockConsumer.addRecord(
                new ConsumerRecord<>(producerRecord.topic(), producerRecord.partition(), 0, producerRecord.key(),
                        producerRecord.value()));
        this.inferenceProducer.send(producerRecord);
        assertThat(this.mockConsumer.committed(Set.of(topicPartition))).isEmpty();
        this.inferenceProducer.send(producerRecord);
        assertThat(this.mockConsumer.committed(Set.of(topicPartition))).isEmpty();
        this.inferenceConsumer.commit();
        assertThat(this.mockConsumer.committed(Set.of(topicPartition)).get(topicPartition).offset()).isEqualTo(2);
    }

}
