package at.raphaell.inference;

import java.util.List;

public record EmbeddedChunkable(ChunkedChunkable chunkedChunkable, List<Float> vector) {

}
