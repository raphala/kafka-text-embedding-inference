package at.raphaell.inference;


import java.util.List;

public interface Chunker {

    List<ChunkedChunkable> chunkText(final Chunkable chunkable);

}

