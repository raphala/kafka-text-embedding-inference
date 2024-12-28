package at.raphaell.inference;

import at.raphaell.inference.models.EmbeddedPaper;
import at.raphaell.inference.models.Paper;
import org.apache.kafka.common.serialization.Serde;

public record InferenceConfig(EmbedClient embedClient, Serde<Paper> inputSerde, Serde<EmbeddedPaper> outputSerde,
                              Chunker chunker) {
}
