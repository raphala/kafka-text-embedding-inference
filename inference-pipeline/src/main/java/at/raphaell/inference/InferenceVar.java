package at.raphaell.inference;

public final class InferenceVar {
    public static final String BATCH_SIZE = "1";
    public static final String GROUP_ID = "kafka-api-inference";
    public static final String BOOTSTRAP_SERVER = "strimzi-k8kafka-kafka-bootstrap.infrastructure.svc:9092";
    public static final String SCHEMA_REGISTRY =
            "http://k8kafka-cp-schema-registry.infrastructure.svc.cluster.local:8081";
    public static final String INPUT_TOPIC = "inference-test-paper";
    public static final String OUTPUT_TOPIC = "inference-test-embedded-paper";
    public static final String TEI_HOST = "text-embeddings.personal-raphael-lachtner.svc.cluster.local";
    public static final int TEI_PORT = 50051;

    private InferenceVar() {
    }
}
