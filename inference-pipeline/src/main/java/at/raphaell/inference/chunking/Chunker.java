package at.raphaell.inference.chunking;

import at.raphaell.inference.models.Chunkable;
import at.raphaell.inference.models.ChunkedChunkable;
import java.util.List;

/**
 * Interface for implementing chunking strategy that chunks text from a {@link Chunkable} into smaller pieces.
 */
@FunctionalInterface
public interface Chunker {

    /**
     * Splits the text of the given {@link Chunkable} into a list of {@link ChunkedChunkable}.
     *
     * @param chunkable input containing the text to be chunked
     * @return a list of chunked text pieces
     */
    List<ChunkedChunkable> chunkText(final Chunkable chunkable);

}

