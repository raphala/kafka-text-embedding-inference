package at.raphaell.inference.chunking;


import at.raphaell.inference.models.Chunkable;
import at.raphaell.inference.models.ChunkedChunkable;
import java.util.List;

@FunctionalInterface
public interface Chunker {

    List<ChunkedChunkable> chunkText(final Chunkable chunkable);

}

