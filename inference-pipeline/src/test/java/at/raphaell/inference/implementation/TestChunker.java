package at.raphaell.inference.implementation;

import at.raphaell.inference.chunking.Chunker;
import at.raphaell.inference.models.Chunkable;
import at.raphaell.inference.models.ChunkedChunkable;
import java.util.List;

public class TestChunker implements Chunker {
    @Override
    public List<ChunkedChunkable> chunkText(final Chunkable chunkable) {
        // This test Chunker does not actually chunk the text, but simply returns the input as a single chunk
        return List.of(new ChunkedChunkable(chunkable, chunkable.getText()));
    }
}
