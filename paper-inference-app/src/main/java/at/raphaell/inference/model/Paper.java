package at.raphaell.inference.model;

import at.raphaell.inference.models.Chunkable;
import com.fasterxml.jackson.annotation.JsonProperty;

public record Paper(
        @JsonProperty("title") String title,
        @JsonProperty("abstract") String abstractText,
        @JsonProperty("doi") String doi
) implements Chunkable {
    @Override
    public String getText() {
        return this.abstractText;
    }
}
