package at.raphaell.inference;

import at.raphaell.inference.chunking.Chunker;
import at.raphaell.inference.models.Chunkable;
import at.raphaell.inference.models.ChunkedChunkable;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple chunker implementation that naively splits text into fixed-size chunks with a specified overlap.
 */
public class NaiveCharacterChunker implements Chunker {

    private final int chunkSize;
    private final int chunkOverlap;

    /**
     * Constructs a {@code NaiveCharacterChunker} with the specified chunk size and overlap. All values refer to
     * characters.
     */
    public NaiveCharacterChunker(final int chunkSize, final int chunkOverlap) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    /**
     * Splits the text from the given {@link Chunkable} into a list of {@link ChunkedChunkable}s. Chunks are created
     * based on the configured chunk size and overlap.
     *
     * @param chunkable input containing the text to be chunked
     * @return a list of chunked text pieces
     */
    @Override
    public List<ChunkedChunkable> chunkText(final Chunkable chunkable) {
        final String inputText = chunkable.getText();
        final List<ChunkedChunkable> chunks = new ArrayList<>();
        final int textLength = inputText.length();

        int start = 0;
        while (start < textLength) {
            final int end = Math.min(start + this.chunkSize, textLength);
            final String chunk = inputText.substring(start, end);
            chunks.add(new ChunkedChunkable(chunkable, chunk));

            start += (this.chunkSize - this.chunkOverlap);
        }

        return chunks;
    }
}
