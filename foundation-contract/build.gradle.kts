plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
}
