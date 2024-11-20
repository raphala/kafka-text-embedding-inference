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
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes.StringSerde;
import org.apache.kafka.streams.Topology;
import picocli.CommandLine.Option;

public class InferenceApp extends KafkaStreamsApplication {

    @Option(names = "--host", description = "the hostname of the grpc inference service", required = true)
    private String host;
    @Option(names = "--port", description = "the port of the grpc inference service", required = true)
    private int port;
    @Option(names = "--chunk-size", description = "the size of individual chunks to embed", required = true)
    private int chunkSize;
    @Option(names = "--chunk-overlap", description = "the overlap between chunks", required = true)
    private int chunkOverlap;

    @Nullable
    private EmbedClient embedClient = null;
    @Nullable
    private Topology topology = null;
    @Nullable
    private Map<String, Object> kafkaProperties = null;

    public static void main(final String[] args) {
        startApplication(new InferenceApp(), args);
    }

    @Override
    public StreamsApp createApp() {
        return new StreamsApp() {
            @Override
            public void buildTopology(final TopologyBuilder builder) {
                InferenceApp.this.embedClient =
                        new EmbedClient(ManagedChannelBuilder.forAddress(InferenceApp.this.host, InferenceApp.this.port)
                                .usePlaintext()
                                .build());
                final Chunker chunker = new Chunker(InferenceApp.this.chunkSize, InferenceApp.this.chunkOverlap);

                final Serde<Paper> paperSerde = SerdeUtils.getSerde(Paper.class, builder.getKafkaProperties());
                final Serde<EmbeddedPaper> embeddedPaperSerde =
                        SerdeUtils.getSerde(EmbeddedPaper.class, builder.getKafkaProperties());
                final InferenceConfig inferenceConfig =
                        new InferenceConfig(InferenceApp.this.embedClient, paperSerde, embeddedPaperSerde, chunker);
                new InferenceTopology(inferenceConfig).buildTopology(builder);
                InferenceApp.this.kafkaProperties = builder.getKafkaProperties();
                InferenceApp.this.topology = builder.getStreamsBuilder().build();
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

    @Nullable
    public Topology getTopology() {
        return this.topology;
    }

    @Nullable
    public Map<String, Object> getKafkaProperties() {
        return this.kafkaProperties;
    }
}
