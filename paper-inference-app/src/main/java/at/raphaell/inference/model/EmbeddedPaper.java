package at.raphaell.inference.model;

import at.raphaell.inference.PaperInferenceApp;
import at.raphaell.inference.models.ChunkedChunkable;
import at.raphaell.inference.models.EmbeddedChunkable;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

public record EmbeddedPaper(
        @JsonProperty("collection_name") String collectionName,
        @JsonProperty("id") String id,
        @JsonProperty("vector") List<Float> vector,
        @JsonProperty("payload") Payload payload
) {

    public static EmbeddedPaper fromEmbeddedChunkable(final EmbeddedChunkable embeddedChunkable) {
        final ChunkedChunkable chunkedChunkable = embeddedChunkable.chunkedChunkable();
        final Paper paper = (Paper) chunkedChunkable.chunkable();
        final Payload paperPayload =
                new Payload(paper.doi(), paper.title(), paper.abstractText(), chunkedChunkable.textChunk());
        return new EmbeddedPaper(PaperInferenceApp.COLLECTION_NAME, UUID.randomUUID().toString(),
                embeddedChunkable.vector(),
                paperPayload);
    }

    private record Payload(
            @JsonProperty("doi") String doi,
            @JsonProperty("title") String title,
            @JsonProperty("abstract") String abstractText,
            @JsonProperty("abstract_chunk") String abstractChunk
    ) {}
}
