plugins {
    id("java")
    id("com.google.cloud.tools.jib") version "3.4.4"
}

version = "0.1.0"

repositories {
    mavenCentral()
    maven { url = uri("https://packages.confluent.io/maven") }
}

dependencies {
    implementation(project(":inference-pipeline"))

    implementation(libs.logback)
    implementation(libs.jackson.core)
    implementation(libs.picocli)
    implementation(libs.confluent.json.serializer)
    implementation(libs.kafka)
    implementation(libs.langchain)
    implementation(libs.langchain.embedding)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testImplementation(libs.mockito)
}

tasks.test {
    useJUnitPlatform()
}

jib {
    container {
        mainClass = "at.raphaell.inference.PaperInferenceApp"
    }
    from {
        image = "eclipse-temurin:23-jre"
    }
    to {
        image = "paper-inference-app:" + project.version
    }
}
