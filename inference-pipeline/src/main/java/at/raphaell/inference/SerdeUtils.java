package at.raphaell.inference;

import io.confluent.kafka.streams.serdes.json.KafkaJsonSchemaSerde;
import java.util.Map;
import org.apache.kafka.common.serialization.Serde;

public class SerdeUtils {

    public static <T> Serde<T> getSerde(final Class<T> clazz, final Map<String, Object> config) {
        final Serde<T> serde = new KafkaJsonSchemaSerde<>(clazz);
        serde.configure(config, false);
        return serde;
    }

}
