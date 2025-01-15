package at.raphaell.inference;

import at.raphaell.inference.models.ChunkedChunkable;
import com.google.common.annotations.VisibleForTesting;
import io.grpc.Channel;
import java.util.List;
import tei.v1.EmbedGrpc;
import tei.v1.EmbedGrpc.EmbedBlockingStub;
import tei.v1.Tei.EmbedRequest;
import tei.v1.Tei.EmbedResponse;

/**
 * Client for embedding text chunks using the Hugging Face text-embeddings-inference gRPC service. See: <a
 * href="https://github.com/huggingface/text-embeddings-inference">https://github
 * .com/huggingface/text-embeddings-inference</a>
 */
public class EmbedClient {

    private final EmbedBlockingStub blockingStub;

    /**
     * Constructs an {@code EmbedClient} with the specified gRPC channel.
     *
     * @param channel gRPC channel to connect to the embedding service
     */
    public EmbedClient(final Channel channel) {
        this.blockingStub = EmbedGrpc.newBlockingStub(channel);
    }

    /**
     * Constructs an {@code EmbedClient} with the specified blocking stub. This is intended for testing purposes only.
     *
     * @param blockingStub blocking stub for the embedding service
     */
    @VisibleForTesting
    public EmbedClient(final EmbedBlockingStub blockingStub) {
        this.blockingStub = blockingStub;
    }

    /**
     * Embeds the given text chunk using the embedding service.
     *
     * @param chunkedChunkable chunk of text to embed
     * @return a list of floats representing the embedding vector
     */
    public List<Float> embed(final ChunkedChunkable chunkedChunkable) {
        final EmbedRequest embedRequest =
                EmbedRequest.newBuilder().setInputs(chunkedChunkable.textChunk()).setTruncate(true).build();
        final EmbedResponse embedResponse = this.blockingStub.embed(embedRequest);
        return embedResponse.getEmbeddingsList();
    }
}
