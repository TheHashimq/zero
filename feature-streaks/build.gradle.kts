plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.2.10"
    `java-library`
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation(project(":foundation-design"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    testImplementation(project(":foundation-design"))
    testImplementation(kotlin("test-junit"))
}
