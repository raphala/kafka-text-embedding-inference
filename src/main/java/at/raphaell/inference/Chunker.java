package at.raphaell.inference;

import at.raphaell.inference.models.Paper;
import java.util.ArrayList;
import java.util.List;

public class Chunker {

    //    TODO make cli var
    private static final int CHUNK_SIZE = 128;
    private static final int CHUNK_OVERLAP = 32;

    public static List<ChunksWithPaper> createChunks(final Paper paper) {
        final String inputText = paper.abstract_();
        final List<ChunksWithPaper> chunks = new ArrayList<>();
        final int textLength = paper.abstract_().length();
        int start = 0;

        while (start < textLength) {
            int end = Math.min(start + CHUNK_SIZE, textLength);

            // Adjust end to avoid breaking words
            if (end < textLength && !Character.isWhitespace(inputText.charAt(end))) {
                while (end > start && !Character.isWhitespace(inputText.charAt(end - 1))) {
                    end--;
                }
            }

            chunks.add(new ChunksWithPaper(inputText.substring(start, end).trim(), paper));
            start = end - CHUNK_OVERLAP;
        }

        return chunks;
    }

    public record ChunksWithPaper(String chunk, Paper paper) {}
}

