package at.raphaell.inference.models;

/**
 * An object that can provide text for chunking.
 */
@FunctionalInterface
public interface Chunkable {

    /**
     * Returns the text to be chunked.
     */
    String getText();

}
