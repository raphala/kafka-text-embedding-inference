package at.raphaell.inference;

import at.raphaell.inference.chunking.Chunker;
import at.raphaell.inference.models.Chunkable;
import at.raphaell.inference.models.ChunkedChunkable;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.onnx.HuggingFaceTokenizer;
import java.util.List;

public class RecursiveTokenChunker implements Chunker {

    private final DocumentSplitter documentSplitter;

    public RecursiveTokenChunker(final int chunkSize, final int chunkOverlap) {
        this.documentSplitter = DocumentSplitters.recursive(chunkSize, chunkOverlap, new HuggingFaceTokenizer());
    }

    @Override
    public List<ChunkedChunkable> chunkText(final Chunkable chunkable) {
        if (chunkable.getText() == null || chunkable.getText().isEmpty()) {
            return List.of();
        }
        final Document document = Document.from(chunkable.getText());
        final List<String> chunks = this.documentSplitter.split(document).stream().map(TextSegment::text).toList();
        return chunks.stream().map(chunk -> new ChunkedChunkable(chunkable, chunk)).toList();
    }
}
