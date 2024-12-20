package com.bakdata.inference;

import io.grpc.Channel;
import java.util.List;
import tei.v1.EmbedGrpc;
import tei.v1.EmbedGrpc.EmbedBlockingStub;
import tei.v1.Tei.EmbedRequest;
import tei.v1.Tei.EmbedResponse;

public class EmbedClient {

    //    TODO use async instead of blocking
    private final EmbedBlockingStub blockingStub;

    public EmbedClient(final Channel channel) {
        this.blockingStub = EmbedGrpc.newBlockingStub(channel);
    }

    public List<Float> embed(final String inputChunk) {
        final EmbedRequest embedRequest = EmbedRequest.newBuilder().setInputs(inputChunk).setTruncate(true).build();
        final EmbedResponse embedResponse = this.blockingStub.embed(embedRequest);
        return embedResponse.getEmbeddingsList();
    }
}
