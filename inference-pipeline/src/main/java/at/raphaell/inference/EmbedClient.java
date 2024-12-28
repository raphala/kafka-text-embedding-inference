package at.raphaell.inference;

import at.raphaell.inference.models.ChunkedChunkable;
import com.google.common.annotations.VisibleForTesting;
import io.grpc.Channel;
import java.util.List;
import tei.v1.EmbedGrpc;
import tei.v1.EmbedGrpc.EmbedBlockingStub;
import tei.v1.Tei.EmbedRequest;
import tei.v1.Tei.EmbedResponse;

public class EmbedClient {

    private final EmbedBlockingStub blockingStub;

    public EmbedClient(final Channel channel) {
        this.blockingStub = EmbedGrpc.newBlockingStub(channel);
    }

    @VisibleForTesting
    public EmbedClient(final EmbedBlockingStub blockingStub) {
        this.blockingStub = blockingStub;
    }

    public List<Float> embed(final ChunkedChunkable chunkedChunkable) {
        final EmbedRequest embedRequest =
                EmbedRequest.newBuilder().setInputs(chunkedChunkable.textChunk()).setTruncate(true).build();
        final EmbedResponse embedResponse = this.blockingStub.embed(embedRequest);
        return embedResponse.getEmbeddingsList();
    }
}
