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
    testImplementation(project(":foundation-testing"))
    testImplementation(kotlin("test-junit"))
}
