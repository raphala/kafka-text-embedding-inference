plugins {
    id("java")
    id("com.google.protobuf") version "0.9.4"
    id("com.google.cloud.tools.jib") version "3.4.4"
}

group = "com.bakdata.clickhouse"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://packages.confluent.io/maven") }
}

dependencies {
    implementation(group = "com.bakdata.kafka", name = "streams-bootstrap-cli", version = "3.0.2")
    implementation("io.grpc:grpc-netty-shaded:1.53.0")
    implementation("io.grpc:grpc-protobuf:1.53.0")
    implementation("io.grpc:grpc-stub:1.53.0")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("com.fasterxml.jackson.core:jackson-core:2.18.1")
    implementation("io.confluent:kafka-streams-json-schema-serde:7.7.1")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
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

jib {
    to {
        image = "us.gcr.io/gcp-bakdata-cluster/kafka-streams-inference-test:" + project.version
    }
}
