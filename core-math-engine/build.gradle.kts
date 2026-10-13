plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.serialization)
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(project(":core-domain"))
    implementation(libs.big.math)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}

tasks.test {
    useJUnit()
}

