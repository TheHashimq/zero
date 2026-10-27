plugins {
    kotlin("jvm")
    `java-library`
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    api(project(":foundation-contract"))
    testImplementation(project(":foundation-testing"))
    testImplementation(kotlin("test-junit"))
}
