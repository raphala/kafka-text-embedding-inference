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

/**
 * An inference application that processes {@link Paper} records by chunking their abstracts, embedding the chunks, and
 * producing the embeddings to an output Kafka topic.
 */
public class PaperInferenceApp extends InferenceApp<String, Paper, EmbeddedPaper> {

    /**
     * Name of the Qdrant database collection to store the embeddings in.
     */
    public static final String COLLECTION_NAME = "embeddings";
    @Option(names = "--chunk-size", defaultValue = "500")
    private int chunkSize;
    @Option(names = "--chunk-overlap", defaultValue = "50")
    private int chunkOverlap;

    /**
     * Constructs a {@code PaperInferenceApp} with JSON serialization configuration. This implementation uses Kafka JSON
     * serializers and deserializers without a schema registry.
     */
    public PaperInferenceApp() {
        super(new SerializationConfig(StringDeserializer.class, KafkaJsonSerializer.class, StringSerializer.class,
                KafkaJsonSerializer.class));
    }

    /**
     * Parses command-line arguments and executes the application.
     */
    public static void main(final String[] args) {
        // use picocli CommandLine to properly parse command line arguments
        final int exitCode = new CommandLine(new PaperInferenceApp()).execute(args);
        System.exit(exitCode);
    }

    /**
     * Creates a {@link Chunker} to split paper abstracts into chunks.
     */
    @Override
    public Chunker createChunker() {
        // The NaiveCharacterChunker is a simple chunker that splits text into chunks of a fixed size with a fixed
        // it was used during testing to get comparable results with other implementations
        // return new NaiveCharacterChunker(this.chunkSize, this.chunkOverlap);
        // For demo purposes, we use the more sophisticated LangChainChunker, based on langchain4j's
        // DocumentByParagraphSplitter
        return new RecursiveTokenChunker(this.chunkSize, this.chunkOverlap);
    }

    /**
     * Returns the Kafka consumer group ID for this specific application.
     */
    @Override
    public String getGroupId() {
        return "paper-inference";
    }

    /**
     * Transforms an {@link EmbeddedChunkable} into an {@link EmbeddedPaper} as Kafka output message format.
     */
    @Override
    public EmbeddedPaper transformToOutputMessage(final EmbeddedChunkable embeddedChunkable) {
        return EmbeddedPaper.fromEmbeddedChunkable(embeddedChunkable);
    }

    /**
     * Provides the deserializer for message keys.
     */
    @Override
    public Deserializer<String> getKeyDeserializer() {
        return new StringDeserializer();
    }

    /**
     * Provides the deserializer for input message values of type {@link Paper}.
     */
    @Override
    public Deserializer<Paper> getInputValueDeserializer() {
        final Properties properties = this.createProducerProperties();
        // this needs to be set in order for the Deserializer to know the type of the deserialized object
        properties.setProperty("json.value.type", Paper.class.getName());
        return SerdeUtils.getConfiguredDeserializer(KafkaJsonDeserializer::new, (Map) properties);
    }


    /**
     * Provides the serializer for message keys.
     */
    @Override
    public Serializer<String> getKeySerializer() {
        return new StringSerializer();
    }

    /**
     * Provides the serializer for output message values of type {@link EmbeddedPaper}.
     */
    @Override
    public Serializer<EmbeddedPaper> getOutputValueSerializer() {
        final Properties properties = this.createProducerProperties();
        // this needs to be set in order for the Serializer to know the type of the serialized object
        properties.setProperty("json.value.type", EmbeddedPaper.class.getName());
        return SerdeUtils.getConfiguredSerializer(KafkaJsonSerializer::new, (Map) properties);
    }

}
