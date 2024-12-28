package at.raphaell.inference.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record EmbeddedPaper(
        @JsonProperty("title") String title,
        @JsonProperty("abstract") String abstract_,
        @JsonProperty("doi") String doi,
        @JsonProperty("vectors") List<Float> vectors
) {

    public static EmbeddedPaper fromPaper(final Paper paper, final List<Float> vectors) {
        return new EmbeddedPaper(paper.title(), paper.abstract_(), paper.doi(), vectors);
    }
}
