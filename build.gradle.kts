plugins {
    kotlin("jvm") version "2.1.20"
}

group = "net.eupixel"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.minestom:minestom-snapshots:1_21_5-2398778b46")
    implementation("net.kyori:adventure-text-minimessage:4.20.0")
    implementation("org.slf4j:slf4j-simple:2.0.6")
}

kotlin {
    jvmToolchain(21)
}