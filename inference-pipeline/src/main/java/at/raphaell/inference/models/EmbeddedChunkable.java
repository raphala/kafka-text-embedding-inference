package at.raphaell.inference.models;

import java.util.List;

/**
 * A chunked and embedded {@link ChunkedChunkable} along with its embedding vector.
 *
 * @param chunkedChunkable chunked text
 * @param vector embedding vector corresponding to the text chunk
 */
public record EmbeddedChunkable(ChunkedChunkable chunkedChunkable, List<Float> vector) {
}
