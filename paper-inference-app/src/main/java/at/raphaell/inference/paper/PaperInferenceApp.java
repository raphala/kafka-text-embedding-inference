package at.raphaell.inference.paper;

import at.raphaell.inference.Chunker;
import at.raphaell.inference.InferenceApp;
import at.raphaell.inference.InferenceConfig;
import at.raphaell.inference.NaiveCharacterChunker;
import at.raphaell.inference.SerdeUtils;
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
        final Deserializer<String> keyDeserializer = new StringDeserializer();
        final Deserializer<Paper> valueDeserializer =
                SerdeUtils.getSerde(Paper.class, (Map) this.getProducerProperties()).deserializer();
        final Serializer<String> keySerializer = new StringSerializer();
        final Serializer<EmbeddedPaper> valueSerializer =
                SerdeUtils.getSerde(Paper.class, (Map) this.getConsumerProperties()).serializer();
        final Chunker chunker = new NaiveCharacterChunker(1000, 50);
        super.InferenceApp(keyDeserializer, valueDeserializer, keySerializer, valueSerializer, chunker);
    }

    public static void main(final String[] args) {
        final PaperInferenceApp paperInferenceApp = new PaperInferenceApp();
    }

    @Override
    public Properties getProducerProperties() {
        final Properties producerProperties = new Properties();
        producerProperties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, InferenceConfig.BOOTSTRAP_SERVER);
        producerProperties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProperties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                KafkaJsonSchemaSerializer.class.getName());
        producerProperties.setProperty(ProducerConfig.ACKS_CONFIG, "all");
        producerProperties.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip");
        producerProperties.setProperty("schema.registry.url", InferenceConfig.SCHEMA_REGISTRY);
        return producerProperties;
    }

    @Override
    public Properties getConsumerProperties() {
        final Properties consumerProperties = new Properties();
        consumerProperties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, InferenceConfig.BOOTSTRAP_SERVER);
        consumerProperties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, InferenceConfig.GROUP_ID);
        consumerProperties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        consumerProperties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                KafkaJsonSchemaSerializer.class.getName());
        consumerProperties.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, InferenceConfig.BATCH_SIZE);
        consumerProperties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProperties.setProperty("schema.registry.url", InferenceConfig.SCHEMA_REGISTRY);
        return consumerProperties;
    }
}
