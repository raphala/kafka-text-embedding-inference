package at.raphaell.inference.paper;

import at.raphaell.inference.InferenceApp;
import at.raphaell.inference.SerdeUtils;
import at.raphaell.inference.chunking.Chunker;
import at.raphaell.inference.chunking.NaiveCharacterChunker;
import at.raphaell.inference.models.EmbeddedChunkable;
import at.raphaell.inference.paper.model.EmbeddedPaper;
import at.raphaell.inference.paper.model.Paper;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import picocli.CommandLine;
import picocli.CommandLine.Option;

public final class PaperInferenceApp extends InferenceApp<String, Paper, EmbeddedPaper> {

    public static final String COLLECTION_NAME = "embeddings";
    public static final String GROUP_ID = "paper-inference-app";
    @Option(names = "--chunk-size", defaultValue = "1000")
    private int chunkSize;
    @Option(names = "--chunk-overlap", defaultValue = "50")
    private int chunkOverlap;

    public static void main(final String[] args) {
        final int exitCode = new CommandLine(new PaperInferenceApp()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Chunker createChunker() {
        return new NaiveCharacterChunker(this.chunkSize, this.chunkOverlap);
    }

    @Override
    public Properties createProducerProperties() {
        final Properties properties = new Properties();
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaJsonSchemaSerializer.class.getName());
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.getInferenceArgs().getBootstrapServer());
        properties.setProperty(ProducerConfig.ACKS_CONFIG, "all");
        properties.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip");
        properties.setProperty("schema.registry.url", this.getInferenceArgs().getSchemaRegistry());
        return properties;
    }

    @Override
    public Properties createConsumerProperties() {
        final Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                KafkaJsonSchemaSerializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.getInferenceArgs().getBootstrapServer());
        properties.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, this.getInferenceArgs().getBatchSize());
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.setProperty("schema.registry.url", this.getInferenceArgs().getSchemaRegistry());
        return properties;
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
