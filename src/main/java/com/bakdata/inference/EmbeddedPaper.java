package com.bakdata.inference;

import static com.bakdata.inference.InferenceApp.COLLECTION_NAME;

import com.bakdata.inference.Chunker.ChunkWithPaper;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record EmbeddedPaper(
        @JsonProperty("collection_name") String collectionName,
        @JsonProperty("id") Object id,
        @JsonProperty("vector") List<Float> vector,
        @JsonProperty("payload") Payload payload
) {

    private record Payload(
            @JsonProperty("doi") String doi,
            @JsonProperty("title") String title,
            @JsonProperty("abstract_chunk") String abstractChunk
    ) {}

    public static EmbeddedPaper fromPaper(final ChunkWithPaper chunk, final List<Float> vector, final String id) {
        return new EmbeddedPaper(COLLECTION_NAME, id, vector, new Payload(chunk.paper().doi(), chunk.paper().title(), chunk.chunk()));
    }
}
