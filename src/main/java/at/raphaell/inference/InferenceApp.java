package at.raphaell.inference;

import at.raphaell.inference.models.EmbeddedPaper;
import at.raphaell.inference.models.Paper;
import com.bakdata.kafka.KafkaStreamsApplication;
import com.bakdata.kafka.SerdeConfig;
import com.bakdata.kafka.StreamsApp;
import com.bakdata.kafka.StreamsTopicConfig;
import com.bakdata.kafka.TopologyBuilder;
import io.confluent.kafka.streams.serdes.json.KafkaJsonSchemaSerde;
import io.grpc.ManagedChannelBuilder;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes.StringSerde;

public class InferenceApp extends KafkaStreamsApplication {

    //    TODO make cli var
    private static final String HOST = "localhost";
    private static final int PORT = 8080;
    private final EmbedClient embedClient;

    private InferenceApp() {
        this.embedClient = new EmbedClient(ManagedChannelBuilder.forAddress(HOST, PORT).build());
    }

    public static void main(final String[] args) {
        startApplication(new InferenceApp(), args);
    }

    @Override
    public StreamsApp createApp() {
        return new StreamsApp() {
            @Override
            public void buildTopology(final TopologyBuilder builder) {
                final Serde<Paper> paperSerde = SerdeUtils.getSerde(Paper.class, builder.getKafkaProperties());
                final Serde<EmbeddedPaper> embeddedPaperSerde =
                        SerdeUtils.getSerde(EmbeddedPaper.class, builder.getKafkaProperties());
                final InferenceConfig inferenceConfig =
                        new InferenceConfig(InferenceApp.this.embedClient, paperSerde, embeddedPaperSerde);
                new InferenceTopology(inferenceConfig).buildTopology(builder);
            }

            @Override
            public String getUniqueAppId(final StreamsTopicConfig topics) {
                return "inference-app-" + topics.getOutputTopic();
            }

            @Override
            public SerdeConfig defaultSerializationConfig() {
                return new SerdeConfig(StringSerde.class, KafkaJsonSchemaSerde.class);
            }
        };
    }
}
