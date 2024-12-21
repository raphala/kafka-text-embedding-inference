package at.raphaell.inference.paper.model;

import at.raphaell.inference.ChunkedChunkable;
import at.raphaell.inference.paper.PaperInferenceApp;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record EmbeddedPaper(
        @JsonProperty("collection_name") String collectionName,
        @JsonProperty("id") Object id,
        @JsonProperty("vector") List<Float> vector,
        @JsonProperty("payload") Payload payload
) {

    public static EmbeddedPaper fromPaper(final ChunkedChunkable chunkedChunkable, final List<Float> vector,
            final String id) {
        final Paper paper = (Paper) chunkedChunkable.chunkable();
        final Payload paperPayload = new Payload(paper.doi(), paper.title(), chunkedChunkable.textChunk());
        return new EmbeddedPaper(PaperInferenceApp.COLLECTION_NAME, id, vector, paperPayload);
    }

    private record Payload(
            @JsonProperty("doi") String doi,
            @JsonProperty("title") String title,
            @JsonProperty("abstract_chunk") String abstractChunk
    ) {}
}
