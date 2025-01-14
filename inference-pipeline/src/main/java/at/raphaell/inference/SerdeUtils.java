package at.raphaell.inference;

import java.util.Map;
import java.util.function.Supplier;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

public final class SerdeUtils {

    private SerdeUtils() {
    }

    public static <T> Serde<T> getConfiguredSerde(final Supplier<Serde<T>> serdeSupplier,
            final Map<String, Object> config) {
        final Serde<T> serde = serdeSupplier.get();
        serde.configure(config, false);
        return serde;
    }

    public static <T> Serializer<T> getConfiguredSerializer(final Supplier<Serializer<T>> supplier,
            final Map<String, Object> config) {
        final Serializer<T> serde = supplier.get();
        serde.configure(config, false);
        return serde;
    }

    public static <T> Deserializer<T> getConfiguredDeserializer(final Supplier<Deserializer<T>> supplier,
            final Map<String, Object> config) {
        final Deserializer<T> serde = supplier.get();
        serde.configure(config, false);
        return serde;
    }

}
