package at.raphaell.inference;

/**
 * Configuration class holding serialization and deserialization classes.
 *
 * @param keyDeserializerClass class for key deserialization
 * @param valueDeserializerClass class for value deserialization
 * @param keySerializerClass class for key serialization
 * @param valueSerializerClass class for value serialization
 */
public record SerializationConfig(Class keyDeserializerClass,
                                  Class valueDeserializerClass,
                                  Class keySerializerClass,
                                  Class valueSerializerClass) {
}
