package at.raphaell.inference;

import at.raphaell.inference.chunking.Chunker;
import at.raphaell.inference.model.EmbeddedPaper;
import at.raphaell.inference.model.Paper;
import at.raphaell.inference.models.EmbeddedChunkable;
import io.confluent.kafka.serializers.KafkaJsonDeserializer;
import io.confluent.kafka.serializers.KafkaJsonSerializer;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import picocli.CommandLine;
import picocli.CommandLine.Option;

public class PaperInferenceApp extends InferenceApp<String, Paper, EmbeddedPaper> {

    public static final String COLLECTION_NAME = "embeddings";
    @Option(names = "--chunk-size", defaultValue = "1000")
    private int chunkSize;
    @Option(names = "--chunk-overlap", defaultValue = "50")
    private int chunkOverlap;

    public PaperInferenceApp() {
        super(new SerializationConfig(StringDeserializer.class, KafkaJsonSerializer.class, StringSerializer.class,
                KafkaJsonSerializer.class));
    }

    public static void main(final String[] args) {
        final int exitCode = new CommandLine(new PaperInferenceApp()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Chunker createChunker() {
        return new NaiveCharacterChunker(this.chunkSize, this.chunkOverlap);
    }

    @Override
    public String getGroupId() {
        return "paper-inference";
    }

    @Override
    public EmbeddedPaper transformToOutputMessage(final EmbeddedChunkable embeddedChunkable) {
        return EmbeddedPaper.fromEmbeddedChunkable(embeddedChunkable);
    }

    @Override
    public Deserializer<String> getKeyDeserializer() {
        return new StringDeserializer();
    }

    @Override
    public Deserializer<Paper> getInputValueDeserializer() {
        final Properties properties = this.createProducerProperties();
        properties.setProperty("json.value.type", Paper.class.getName());
        return SerdeUtils.getConfiguredDeserializer(() -> new KafkaJsonDeserializer<>(), (Map) properties);
    }

    @Override
    public Serializer<String> getKeySerializer() {
        return new StringSerializer();
    }

    @Override
    public Serializer<EmbeddedPaper> getOutputValueSerializer() {
        final Properties properties = this.createProducerProperties();
        properties.setProperty("json.value.type", EmbeddedPaper.class.getName());
        return SerdeUtils.getConfiguredSerializer(() -> new KafkaJsonSerializer<>(), (Map) properties);
    }

}
