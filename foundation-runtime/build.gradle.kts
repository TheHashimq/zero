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
    api(project(":foundation-persistence"))
    api(project(":foundation-events"))
    testImplementation(project(":foundation-testing"))
    testImplementation(project(":foundation-registry"))
    testImplementation(kotlin("test-junit"))
}
