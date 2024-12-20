package com.bakdata.inference;

import java.util.ArrayList;
import java.util.List;

public class Chunker {

//    TODO make abstract chunker with one naive and one actual token based chunking

    private final int chunkSize;
    private final int chunkOverlap;

    public Chunker(final int chunkSize, final int chunkOverlap) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    public List<ChunkWithPaper> createChunks(final Paper paper) {
        final String inputText = paper.abstract_();
        final List<ChunkWithPaper> chunks = new ArrayList<>();
        final int textLength = inputText.length();

        int start = 0;
        while (start < textLength) {
            final int end = Math.min(start + this.chunkSize, textLength);
            final String chunk = inputText.substring(start, end);
            chunks.add(new ChunkWithPaper(chunk, paper));

            start += (this.chunkSize - this.chunkOverlap);
        }

        return chunks;
    }

    public record ChunkWithPaper(String chunk, Paper paper) {}
}

