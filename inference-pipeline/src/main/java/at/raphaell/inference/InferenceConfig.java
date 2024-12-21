package at.raphaell.inference;

import at.raphaell.inference.chunking.Chunker;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

public record InferenceConfig<Key, InputValue, OutputValue>(Deserializer<Key> keyDeserializer,
                                                            Deserializer<InputValue> valueDeserializer,
                                                            Serializer<Key> keySerializer,
                                                            Serializer<OutputValue> valueSerializer,
                                                            Chunker chunker) {
}
