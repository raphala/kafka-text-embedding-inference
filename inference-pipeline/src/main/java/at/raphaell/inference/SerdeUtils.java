package at.raphaell.inference;

import java.util.Map;
import java.util.function.Supplier;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

/**
 * Utility methods for configuring Kafka serializers and deserializers.
 */
public final class SerdeUtils {

    private SerdeUtils() {
        // private constructor to prevent instantiation
    }

    /**
     * Configures and returns a Serde.
     *
     * @param serdeSupplier supplier that instantiates a Serde
     * @param config configuration map
     * @param <T> type handled by the Serde
     * @return the configured Serde
     */
    public static <T> Serde<T> getConfiguredSerde(final Supplier<Serde<T>> serdeSupplier,
            final Map<String, Object> config) {
        final Serde<T> serde = serdeSupplier.get();
        serde.configure(config, false);
        return serde;
    }

    /**
     * Configures and returns a Serializer.
     *
     * @param supplier supplier that instantiates a Serializer
     * @param config configuration map
     * @param <T> type handled by the Serializer
     * @return configured Serializer
     */
    public static <T> Serializer<T> getConfiguredSerializer(final Supplier<Serializer<T>> supplier,
            final Map<String, Object> config) {
        final Serializer<T> serde = supplier.get();
        serde.configure(config, false);
        return serde;
    }

    /**
     * Configures and returns a Deserializer.
     *
     * @param supplier supplier that instantiates a Deserializer
     * @param config configuration map
     * @param <T> type handled by the Deserializer
     * @return configured Deserializer
     */
    public static <T> Deserializer<T> getConfiguredDeserializer(final Supplier<Deserializer<T>> supplier,
            final Map<String, Object> config) {
        final Deserializer<T> serde = supplier.get();
        serde.configure(config, false);
        return serde;
    }

}
