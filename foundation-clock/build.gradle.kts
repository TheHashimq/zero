plugins {
    kotlin("jvm")
    `java-library`
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    testImplementation(project(":foundation-testing"))
    testImplementation(kotlin("test-junit"))
}
