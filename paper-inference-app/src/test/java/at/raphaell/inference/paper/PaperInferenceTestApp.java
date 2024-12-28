package at.raphaell.inference.paper;

import static org.mockito.ArgumentMatchers.any;

import at.raphaell.inference.EmbedClient;
import at.raphaell.inference.InferenceConsumer;
import at.raphaell.inference.InferenceProducer;
import at.raphaell.inference.models.ChunkedChunkable;
import at.raphaell.inference.paper.model.EmbeddedPaper;
import at.raphaell.inference.paper.model.Paper;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializer;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.mockito.Mockito;

public class PaperInferenceTestApp extends PaperInferenceApp {

    private static final List<Float> mockEmbedding = List.of(1.0f, 2.0f, 3.0f, 4.0f);
    private MockProducer<String, EmbeddedPaper> mockProducer;
    private MockConsumer<String, Paper> mockConsumer;

    public MockProducer<String, EmbeddedPaper> getMockProducer() {
        return this.mockProducer;
    }

    public MockConsumer<String, Paper> getMockConsumer() {
        return this.mockConsumer;
    }

    @Override
    protected void initApp() {
        this.mockProducer = new MockProducer<>(true, this.getKeySerializer(), this.getOutputValueSerializer());
        this.mockConsumer = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
        this.producerProperties = new Properties();
        this.producerProperties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        this.producerProperties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                KafkaJsonSchemaDeserializer.class.getName());
        this.producerProperties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "mock://kafka");
        this.producerProperties.setProperty(ProducerConfig.ACKS_CONFIG, "all");
        this.producerProperties.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip");
        this.inferenceConsumer = new InferenceConsumer<>(this.mockConsumer, "input");
        this.inferenceProducer = new InferenceProducer<>(this.mockProducer, this.inferenceConsumer);
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        final EmbedClient mockEmbedClient = Mockito.mock(EmbedClient.class);
        Mockito.when(mockEmbedClient.embed(any(ChunkedChunkable.class))).thenReturn(mockEmbedding);
        this.embedClient = mockEmbedClient;
        this.chunker = this.createChunker();
    }
}
