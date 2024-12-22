package at.raphaell.inference.paper;

import at.raphaell.inference.InferenceApp;
import at.raphaell.inference.SerdeUtils;
import at.raphaell.inference.SerializationConfig;
import at.raphaell.inference.chunking.Chunker;
import at.raphaell.inference.models.EmbeddedChunkable;
import at.raphaell.inference.paper.model.EmbeddedPaper;
import at.raphaell.inference.paper.model.Paper;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer;
import java.util.Map;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import picocli.CommandLine;
import picocli.CommandLine.Option;

public final class PaperInferenceApp extends InferenceApp<String, Paper, EmbeddedPaper> {

    public static final String COLLECTION_NAME = "embeddings";
    @Option(names = "--chunk-size", defaultValue = "1000")
    private int chunkSize;
    @Option(names = "--chunk-overlap", defaultValue = "50")
    private int chunkOverlap;

    private PaperInferenceApp() {
        super(new SerializationConfig(StringDeserializer.class, KafkaJsonSchemaSerializer.class, StringSerializer.class,
                KafkaJsonSchemaSerializer.class));
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
    public EmbeddedPaper transformMessage(final EmbeddedChunkable embeddedChunkable) {
        return EmbeddedPaper.fromEmbeddedChunkable(embeddedChunkable);
    }

    @Override
    public Deserializer<String> getKeyDeserializer() {
        return new StringDeserializer();
    }

    @Override
    public Deserializer<Paper> getInputValueDeserializer() {
        return SerdeUtils.getSerde(Paper.class, (Map) this.createProducerProperties()).deserializer();
    }

    @Override
    public Serializer<String> getKeySerializer() {
        return new StringSerializer();
    }

    @Override
    public Serializer<EmbeddedPaper> getOutputValueSerializer() {
        return SerdeUtils.getSerde(Paper.class, (Map) this.createConsumerProperties()).serializer();
    }

}
