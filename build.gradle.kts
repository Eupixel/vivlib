plugins {
    kotlin("jvm") version "2.1.20"
    id("com.google.protobuf") version "0.9.5"
}

group = "net.eupixel"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("net.minestom:minestom-snapshots:1_21_5-2398778b46")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
}

kotlin {
    jvmToolchain(21)
}