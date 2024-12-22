package at.raphaell.inference;

public record SerializationConfig(Class keyDeserializerClass,
                                  Class valueDeserializerClass,
                                  Class keySerializerClass,
                                  Class valueSerializerClass) {
}
