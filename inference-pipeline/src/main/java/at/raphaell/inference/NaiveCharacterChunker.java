package at.raphaell.inference;

import java.util.ArrayList;
import java.util.List;

public class NaiveCharacterChunker implements Chunker {

    private final int chunkSize;
    private final int chunkOverlap;

    public NaiveCharacterChunker(final int chunkSize, final int chunkOverlap) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

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
