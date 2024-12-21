package at.raphaell.inference.paper;

import at.raphaell.inference.InferenceApp;
import at.raphaell.inference.InferenceConfig;
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

public final class PaperInferenceApp extends InferenceApp<String, Paper, EmbeddedPaper> {

    // TODO add all vars to env/cli

    public static final String COLLECTION_NAME = "embeddings";

    private PaperInferenceApp() {
        super(createInferenceConfig());
    }

    public static void main(final String[] args) {
        final PaperInferenceApp paperInferenceApp = new PaperInferenceApp();
        paperInferenceApp.start();
    }

    public static InferenceConfig<String, Paper, EmbeddedPaper> createInferenceConfig() {
        final Deserializer<String> keyDeserializer = new StringDeserializer();
        final Deserializer<Paper> valueDeserializer =
                SerdeUtils.getSerde(Paper.class, (Map) getProducerProperties()).deserializer();
        final Serializer<String> keySerializer = new StringSerializer();
        final Serializer<EmbeddedPaper> valueSerializer =
                SerdeUtils.getSerde(Paper.class, (Map) getConsumerProperties()).serializer();
        final Chunker chunker = new NaiveCharacterChunker(1000, 50);
        return new InferenceConfig<>(keyDeserializer, valueDeserializer, keySerializer, valueSerializer, chunker);
    }

    private static Properties getProducerProperties() {
        final Properties properties = createBaseProducerProperties();
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaJsonSchemaSerializer.class.getName());
        return properties;
    }

    private static Properties getConsumerProperties() {
        final Properties properties = createBaseConsumerProperties();
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                KafkaJsonSchemaSerializer.class.getName());
        return properties;
    }

    @Override
    public EmbeddedPaper transformMessage(final EmbeddedChunkable embeddedChunkable) {
        return EmbeddedPaper.fromEmbeddedChunkable(embeddedChunkable);
    }

}
