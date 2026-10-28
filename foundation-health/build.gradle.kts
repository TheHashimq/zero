plugins {
    kotlin("jvm")
    `java-library`
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    api(project(":foundation-contract"))
    api(project(":foundation-clock"))
    testImplementation(project(":foundation-testing"))
    testImplementation(kotlin("test-junit"))
}
