package at.raphaell.inference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import at.raphaell.inference.implementation.TestChunkable;
import at.raphaell.inference.implementation.TestInferenceApp;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class InferenceAppTest {

    private static final String INPUT_TOPIC = "input";
    private static final String OUTPUT_TOPIC = "output";
    private TestInferenceApp testApp;

    @BeforeEach
    void setup() throws InterruptedException {
        // Set up the test inference app
        this.testApp = new TestInferenceApp();
        final String[] args = {
                "--input-topic", INPUT_TOPIC,
                "--output-topic", OUTPUT_TOPIC,
                "--batch-size", "1",
                "--bootstrap-server", "mock://localhost:9092",
                "--tei-host", "mock://localhost",
                "--tei-port", "50051"
        };
        final Thread thread = new Thread(() -> {
            final int cmd = new CommandLine(this.testApp).execute(args);
        });
        thread.setDaemon(true);
        thread.start();
        Thread.sleep(1000);
    }

    @Test
    void shouldProcessRecords() throws InterruptedException {
        // Create two records with Chunkable values and add them to the input topic
        final ConsumerRecord<String, TestChunkable> record1 =
                new ConsumerRecord<>(INPUT_TOPIC, 0, 0L, "key1", new TestChunkable("chunkable1"));
        final ConsumerRecord<String, TestChunkable> record2 =
                new ConsumerRecord<>(INPUT_TOPIC, 0, 1L, "key2", new TestChunkable("chunkable2"));
        final TopicPartition topicPartition = new TopicPartition(INPUT_TOPIC, 0);
        this.testApp.getMockConsumer().updateBeginningOffsets(Map.of(topicPartition, 0L));
        this.testApp.getMockConsumer().rebalance(List.of(topicPartition));
        this.testApp.getMockConsumer().addRecord(record1);
        this.testApp.getMockConsumer().addRecord(record2);

        // Wait for the records to be processed
        Thread.sleep(1000);

        final List<ProducerRecord<String, String>> producedRecords = this.testApp.getMockProducer().history();

        // Assert that two records matching the input were produced to the output topic
        assertThat(producedRecords).hasSize(2);

        assertThat(producedRecords)
                .extracting(record -> tuple(record.topic(), record.key(), record.value()))
                .containsExactlyInAnyOrder(
                        tuple(OUTPUT_TOPIC, record1.key(), record1.value().chunk()),
                        tuple(OUTPUT_TOPIC, record2.key(), record2.value().chunk())
                );
    }

}
