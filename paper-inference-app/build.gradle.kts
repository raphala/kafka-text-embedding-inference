plugins {
    id("java")
    id("com.google.cloud.tools.jib") version "3.4.4"
}

version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://packages.confluent.io/maven") }
}

dependencies {
    implementation(project(":inference-pipeline"))
    implementation("ch.qos.logback:logback-classic:1.5.13")
    implementation("com.fasterxml.jackson.core:jackson-core:2.18.1")
    implementation("io.confluent:kafka-streams-json-schema-serde:7.7.1")
    implementation("info.picocli:picocli:4.7.6")
}

tasks.test {
    useJUnitPlatform()
}

jib {
    to {
        image = "us.gcr.io/gcp-bakdata-cluster/kafka-api-inference-test:" + project.version
    }
}
