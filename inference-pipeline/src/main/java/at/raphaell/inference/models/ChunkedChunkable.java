package at.raphaell.inference.models;

/**
 * A chunked piece of text from a {@link Chunkable}.
 *
 * @param chunkable original source of the text
 * @param textChunk chunked part of the text
 */
public record ChunkedChunkable(Chunkable chunkable, String textChunk) {
}
