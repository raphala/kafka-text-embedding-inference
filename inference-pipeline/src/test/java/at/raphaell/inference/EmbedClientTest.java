package at.raphaell.inference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import at.raphaell.inference.implementation.TestChunkable;
import at.raphaell.inference.models.ChunkedChunkable;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import tei.v1.EmbedGrpc;
import tei.v1.Tei.EmbedRequest;
import tei.v1.Tei.EmbedResponse;

class EmbedClientTest {

    @Mock
    private EmbedGrpc.EmbedBlockingStub blockingStub = mock(EmbedGrpc.EmbedBlockingStub.class);
    private final EmbedClient embedClient = new EmbedClient(this.blockingStub);

    @Test
    void testEmbed() {
        final String text = "test text";
        final ChunkedChunkable chunkedChunkable = new ChunkedChunkable(new TestChunkable(text), text);
        final EmbedRequest expectedRequest = EmbedRequest.newBuilder()
                .setInputs(chunkedChunkable.textChunk())
                .setTruncate(true)
                .build();

        final List<Float> mockEmbeddings = Arrays.asList(0.1f, 0.2f, 0.3f);
        final EmbedResponse mockResponse = EmbedResponse.newBuilder()
                .addAllEmbeddings(mockEmbeddings)
                .build();

        when(this.blockingStub.embed(expectedRequest)).thenReturn(mockResponse);

        final List<Float> result = this.embedClient.embed(chunkedChunkable);

        assertThat(mockEmbeddings).isEqualTo(result);
        verify(this.blockingStub).embed(expectedRequest);
    }

}
