plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation(project(":foundation-contract"))
    testImplementation(project(":foundation-testing"))
    testImplementation(kotlin("test-junit"))
}
