package at.raphaell.inference.paper;

import static org.assertj.core.api.Assertions.assertThat;

import at.raphaell.inference.paper.model.EmbeddedPaper;
import at.raphaell.inference.paper.model.Paper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

public class PaperInferenceAppTest {

    private static final String INPUT_TOPIC = "input";
    private static final String OUTPUT_TOPIC = "expected";
    private static final Path RESOURCES = Paths.get("src/test/resources");
    private PaperInferenceTestApp testApp;

    @BeforeEach
    void setup() throws InterruptedException {
        this.testApp = new PaperInferenceTestApp();
        final String[] args = {
                "--input-topic", INPUT_TOPIC,
                "--output-topic", OUTPUT_TOPIC,
                "--batch-size", "1",
                "--bootstrap-server", "mock://mock",
                "--schema-registry", "mock://mock",
                "--tei-host", "mock://mock",
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
    void shouldProduceChunkedAndEmbeddedPaper() throws InterruptedException, IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final Paper paper = mapper.readValue(new File(String.valueOf(RESOURCES.resolve("paper.json"))), Paper.class);
        final ConsumerRecord<String, Paper> paperRecord =
                new ConsumerRecord<>(INPUT_TOPIC, 0, 0L, "key1", paper);
        final TopicPartition topicPartition = new TopicPartition(INPUT_TOPIC, 0);
        this.testApp.getMockConsumer().updateBeginningOffsets(Map.of(topicPartition, 0L));
        this.testApp.getMockConsumer().rebalance(List.of(topicPartition));
        this.testApp.getMockConsumer().addRecord(paperRecord);

        Thread.sleep(1000);

        final List<ProducerRecord<String, EmbeddedPaper>> producedRecords = this.testApp.getMockProducer().history();

        assertThat(producedRecords).hasSize(2);

        final List<EmbeddedPaper> expected = List.of(
                mapper.readValue(new File(String.valueOf(RESOURCES.resolve("expected/embeddedpaper_1.json"))),
                        EmbeddedPaper.class),
                mapper.readValue(new File(String.valueOf(RESOURCES.resolve("expected/embeddedpaper_2.json"))),
                        EmbeddedPaper.class)
        );

        assertThat(producedRecords.stream().map(record -> record.value()))
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
                .containsExactlyInAnyOrderElementsOf(expected);
    }
}
