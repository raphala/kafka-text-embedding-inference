package at.raphaell.inference;

import at.raphaell.inference.models.Paper;
import java.util.ArrayList;
import java.util.List;

public class Chunker {

    //    TODO make var
    private static final int CHUNK_SIZE = 128;
    private static final int CHUNK_OVERLAP = 32;

    public static List<ChunksWithPaper> createChunks(final Paper paper) {
        final String inputText = paper.abstract_();
        final List<ChunksWithPaper> chunks = new ArrayList<>();


        int textLength = inputText.length();
        int start = 0;

        while (start < textLength) {
            int end = Math.min(start + CHUNK_SIZE, textLength);
            String chunk = inputText.substring(start, end);
            chunks.add(new ChunksWithPaper(chunk, paper));

            start += (CHUNK_SIZE - CHUNK_OVERLAP);
        }

        return chunks;
    }

    public record ChunksWithPaper(String chunk, Paper paper) {}
}

