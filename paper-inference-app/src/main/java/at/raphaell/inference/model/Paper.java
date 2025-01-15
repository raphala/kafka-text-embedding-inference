package at.raphaell.inference.model;

import at.raphaell.inference.models.Chunkable;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON schema that represents a research paper with a title, abstract, and DOI. Implements {@link Chunkable} to provide
 * text for chunking.
 */
public record Paper(
        @JsonProperty("title") String title,
        @JsonProperty("abstract") String abstractText,
        @JsonProperty("doi") String doi
) implements Chunkable {

    /**
     * Returns the abstract text of the paper for chunking.
     */
    @Override
    public String getText() {
        return this.abstractText;
    }
}
