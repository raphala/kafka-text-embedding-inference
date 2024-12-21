package at.raphaell.inference.models;

import java.util.List;

public record EmbeddedChunkable(ChunkedChunkable chunkedChunkable, List<Float> vector) {

}
