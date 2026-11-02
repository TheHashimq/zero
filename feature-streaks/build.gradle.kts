plugins {
    kotlin("jvm")
    `java-library`
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation(project(":foundation-design"))
    testImplementation(project(":foundation-design"))
    testImplementation(kotlin("test-junit"))
}
