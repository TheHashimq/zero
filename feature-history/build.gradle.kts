plugins {
    kotlin("jvm")
    `java-library`
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    api(project(":foundation-runtime"))
    testImplementation(project(":foundation-testing"))
    testImplementation(project(":foundation-registry"))
    testImplementation(kotlin("test-junit"))
}
