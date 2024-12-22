package at.raphaell.inference;

import at.raphaell.inference.chunking.Chunker;

public record InferenceConfig<Key, InputValue, OutputValue>(
//        Properties consumerProperties,
//                                                            Properties producerProperties,
//                                                            Deserializer<Key> keyDeserializer,
//                                                            Deserializer<InputValue> valueDeserializer,
//                                                            Serializer<Key> keySerializer,
//                                                            Serializer<OutputValue> valueSerializer,
        Chunker chunker) {
}
