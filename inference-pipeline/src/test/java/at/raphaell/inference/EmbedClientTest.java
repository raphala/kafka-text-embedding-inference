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

    // Create a mock of the EmbedGrpc.EmbedBlockingStub class
    @Mock
    private EmbedGrpc.EmbedBlockingStub blockingStub = mock(EmbedGrpc.EmbedBlockingStub.class);
    // Pass the mock to the EmbedClient constructor
    private final EmbedClient embedClient = new EmbedClient(this.blockingStub);

    @Test
    void testEmbed() {
        final String text = "test text";
        final ChunkedChunkable chunkedChunkable = new ChunkedChunkable(new TestChunkable(text), text);
        final EmbedRequest expectedRequest = EmbedRequest.newBuilder()
                .setInputs(chunkedChunkable.textChunk())
                .setTruncate(true)
                .build();

        // Create a mock response with a list of floats
        final List<Float> mockEmbeddings = Arrays.asList(0.1f, 0.2f, 0.3f);
        final EmbedResponse mockResponse = EmbedResponse.newBuilder()
                .addAllEmbeddings(mockEmbeddings)
                .build();

        // Mock the embed method of the blocking stub to return the mock response
        when(this.blockingStub.embed(expectedRequest)).thenReturn(mockResponse);

        final List<Float> result = this.embedClient.embed(chunkedChunkable);

        // Assert that the result is equal to the mock response
        assertThat(mockEmbeddings).isEqualTo(result);
        // Verify that the embed method of the blocking stub was called with the expected request
        verify(this.blockingStub).embed(expectedRequest);
    }

}
