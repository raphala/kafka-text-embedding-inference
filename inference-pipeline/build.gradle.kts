plugins {
    id("java")
    id("com.google.protobuf") version "0.9.4"
}

version = "0.1.0"

repositories {
    mavenCentral()
    maven { url = uri("https://packages.confluent.io/maven") }
}

dependencies {
    implementation("org.apache.kafka:kafka-clients:3.9.0")
    implementation("io.grpc:grpc-netty-shaded:1.53.0")
    implementation("io.grpc:grpc-protobuf:1.53.0")
    implementation("io.grpc:grpc-stub:1.53.0")
    implementation("io.confluent:kafka-streams-json-schema-serde:7.7.1")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("ch.qos.logback:logback-classic:1.5.13")
    implementation("info.picocli:picocli:4.7.6")
    implementation("com.google.guava:guava:33.4.0-jre")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("org.mockito:mockito-core:5.14.2")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.21.7"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.53.0"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                create("grpc")
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

