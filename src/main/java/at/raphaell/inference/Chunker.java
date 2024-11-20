package at.raphaell.inference;

import at.raphaell.inference.models.Paper;
import java.util.ArrayList;
import java.util.List;

public class Chunker {

    private final int chunkSize;
    private final int chunkOverlap;

    public Chunker(final int chunkSize, final int chunkOverlap) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    public List<ChunksWithPaper> createChunks(final Paper paper) {
        final String inputText = paper.abstract_();
        final List<ChunksWithPaper> chunks = new ArrayList<>();
        final int textLength = inputText.length();

        int start = 0;
        while (start < textLength) {
            final int end = Math.min(start + this.chunkSize, textLength);
            final String chunk = inputText.substring(start, end);
            chunks.add(new ChunksWithPaper(chunk, paper));

            start += (this.chunkSize - this.chunkOverlap);
        }

        return chunks;
    }

    public record ChunksWithPaper(String chunk, Paper paper) {}
}

