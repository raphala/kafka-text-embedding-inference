package at.raphaell.inference.implementation;

import at.raphaell.inference.models.Chunkable;

public record TestChunkable(String chunk) implements Chunkable {
    @Override
    public String getText() {
        return this.chunk;
    }
}
