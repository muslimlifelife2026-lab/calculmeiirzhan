plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.serialization)
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    testImplementation(kotlin("test"))
}
