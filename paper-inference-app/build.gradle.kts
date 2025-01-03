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

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("org.mockito:mockito-core:5.14.2")
}

tasks.test {
    useJUnitPlatform()
}

jib {
    container {
        mainClass = "at.raphaell.inference.paper.PaperInferenceApp"
    }
    from {
        image = "eclipse-temurin:23-jre"
    }
    to {
        image = "paper-inference-app:" + project.version
    }
}
