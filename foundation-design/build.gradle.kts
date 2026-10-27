plugins {
    kotlin("jvm")
    `java-library`
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    testImplementation(kotlin("test-junit"))
}
