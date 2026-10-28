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
    api(project(":foundation-health"))
    api(project(":foundation-navigation"))
    testImplementation(project(":foundation-registry"))
    testImplementation(project(":foundation-testing"))
    testImplementation(kotlin("test-junit"))
}
