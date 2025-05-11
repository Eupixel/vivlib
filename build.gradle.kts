import com.google.protobuf.gradle.*

plugins {
    kotlin("jvm") version "2.1.20"
    id("com.google.protobuf") version "0.9.5"
}

group = "net.eupixel"
version = "1.0"

repositories {
    mavenCentral()
}

protobuf {
    protoc { artifact = "com.google.protobuf:protoc:3.24.1" }
    plugins {
        id("grpc")   { artifact = "io.grpc:protoc-gen-grpc-java:1.59.0" }
        id("grpckt") { artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.0:jdk7@jar" }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                id("grpc")
                id("grpckt")
            }
        }
    }
}

dependencies {
    implementation("io.grpc:grpc-kotlin-stub:1.4.0")
    implementation("io.grpc:grpc-netty-shaded:1.59.0")
    implementation("com.google.protobuf:protobuf-kotlin:3.24.1")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
}

kotlin {
    jvmToolchain(21)
}