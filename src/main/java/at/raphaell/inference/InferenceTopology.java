package at.raphaell.inference;

import at.raphaell.inference.models.EmbeddedPaper;
import at.raphaell.inference.models.Paper;
import com.bakdata.kafka.TopologyBuilder;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;

public record InferenceTopology(InferenceConfig inferenceConfig) {

    public void buildTopology(final TopologyBuilder builder) {
        final KStream<String, Paper> input =
                builder.streamInput(Consumed.with(Serdes.String(), this.inferenceConfig.inputSerde()));

//        TODO Error handling
        final KStream<String, EmbeddedPaper> embeddedPapers =
                input.flatMapValues(this.inferenceConfig.chunker()::createChunks)
                        .mapValues(value -> EmbeddedPaper.fromPaper(value.paper(),
                                this.inferenceConfig.embedClient().embed(value.chunk())));

        embeddedPapers.to(builder.getTopics().getOutputTopic(),
                Produced.with(Serdes.String(), this.inferenceConfig.outputSerde()));
    }

}
