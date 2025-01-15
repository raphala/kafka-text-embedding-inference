package at.raphaell.inference;

import picocli.CommandLine.Option;

/**
 * Command-line arguments for configuring the inference application.
 */
public class InferenceArgs {

    @Option(names = "--batch-size", defaultValue = "1")
    private String batchSize;
    @Option(names = "--bootstrap-server")
    private String bootstrapServer;
    @Option(names = "--schema-registry")
    private String schemaRegistry;
    @Option(names = "--input-topic")
    private String inputTopic;
    @Option(names = "--output-topic")
    private String outputTopic;
    @Option(names = "--tei-host")
    private String teiHost;
    @Option(names = "--tei-port", defaultValue = "50051")
    private int teiPort;

    public String getBatchSize() {
        return this.batchSize;
    }

    public String getBootstrapServer() {
        return this.bootstrapServer;
    }

    public String getSchemaRegistry() {
        return this.schemaRegistry;
    }

    public String getInputTopic() {
        return this.inputTopic;
    }

    public String getOutputTopic() {
        return this.outputTopic;
    }

    public String getTeiHost() {
        return this.teiHost;
    }

    public int getTeiPort() {
        return this.teiPort;
    }
}
