package at.raphaell.inference.implementation;

import static org.mockito.ArgumentMatchers.any;

import at.raphaell.inference.EmbedClient;
import at.raphaell.inference.InferenceApp;
import at.raphaell.inference.InferenceConsumer;
import at.raphaell.inference.InferenceProcessor;
import at.raphaell.inference.InferenceProducer;
import at.raphaell.inference.SerdeUtils;
import at.raphaell.inference.SerializationConfig;
import at.raphaell.inference.chunking.Chunker;
import at.raphaell.inference.models.ChunkedChunkable;
import at.raphaell.inference.models.EmbeddedChunkable;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer;
import io.confluent.kafka.streams.serdes.json.KafkaJsonSchemaSerde;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.mockito.Mockito;

public class TestInferenceApp extends InferenceApp<String, TestChunkable, String> {

    private static final List<Float> mockEmbedding = List.of(1.0f, 2.0f, 3.0f, 4.0f);
    private MockProducer<String, String> mockProducer;
    private MockConsumer<String, TestChunkable> mockConsumer;

    public TestInferenceApp() {
        super(new SerializationConfig(StringDeserializer.class, KafkaJsonSchemaSerializer.class, StringSerializer.class,
                StringSerializer.class));
    }

    @Override
    public String transformToOutputMessage(final EmbeddedChunkable embeddedChunkable) {
        return embeddedChunkable.chunkedChunkable().textChunk();
    }

    @Override
    public Chunker createChunker() {
        return new TestChunker();
    }

    @Override
    public String getGroupId() {
        return "test-group";
    }

    @Override
    public Deserializer<String> getKeyDeserializer() {
        return new StringDeserializer();
    }

    @Override
    public Deserializer<TestChunkable> getInputValueDeserializer() {
        return SerdeUtils.getConfiguredSerde(() -> new KafkaJsonSchemaSerde<>(TestChunkable.class),
                (Map) this.createProducerProperties()).deserializer();
    }

    @Override
    public Serializer<String> getKeySerializer() {
        return new StringSerializer();
    }

    @Override
    public Serializer<String> getOutputValueSerializer() {
        return new StringSerializer();
    }

    public MockProducer<String, String> getMockProducer() {
        return this.mockProducer;
    }

    public MockConsumer<String, TestChunkable> getMockConsumer() {
        return this.mockConsumer;
    }

    @Override
    protected void initApp() {
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
        final EmbedClient mockEmbedClient = Mockito.mock(EmbedClient.class);
        Mockito.when(mockEmbedClient.embed(any(ChunkedChunkable.class))).thenReturn(mockEmbedding);
        this.embedClient = mockEmbedClient;
        this.chunker = this.createChunker();
        this.inferenceProcessor = new InferenceProcessor(this);
    }
}
