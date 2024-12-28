package at.raphaell.inference;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InferenceConsumerTest {

    private MockConsumer<String, String> mockConsumer;
    private InferenceConsumer<String, String> inferenceConsumer;

    @BeforeEach
    void setup() {
        this.mockConsumer = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
        this.inferenceConsumer = new InferenceConsumer<>(this.mockConsumer, "input");
    }

    @Test
    void shouldPollRecord() {
        final TopicPartition topicPartition = new TopicPartition("output", 0);
        final ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("output", 0, 1, "key", "value");
        this.mockConsumer.rebalance(List.of(topicPartition));
        this.mockConsumer.updateBeginningOffsets(Map.of(topicPartition, 0L));
        this.mockConsumer.addRecord(consumerRecord);
        final ConsumerRecords<String, String> consumerRecords = this.inferenceConsumer.poll();
        assertThat(consumerRecords).hasSize(1);
        assertThat(consumerRecords.records("output").iterator().next()).isEqualTo(consumerRecord);
    }

    @Test
    void shouldPollMultipleRecords() {
        final TopicPartition topicPartition = new TopicPartition("output", 0);
        this.mockConsumer.rebalance(List.of(topicPartition));
        this.mockConsumer.updateBeginningOffsets(Map.of(topicPartition, 0L));
        this.mockConsumer.addRecord(new ConsumerRecord<>("output", 0, 1, "key", "value"));
        this.mockConsumer.addRecord(new ConsumerRecord<>("output", 0, 2, "key", "value"));
        this.mockConsumer.addRecord(new ConsumerRecord<>("output", 0, 3, "key", "value"));
        final ConsumerRecords<String, String> consumerRecords = this.inferenceConsumer.poll();
        assertThat(consumerRecords).hasSize(3);
    }
}
