plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

val releaseStorePath = providers.gradleProperty("RELEASE_STORE_FILE").orElse("")

android {
    namespace = "com.aerocalc.smartcalculator"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.aerocalc.smartcalculator"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val productionAppId = providers.gradleProperty("ADMOB_APP_ID").orElse("").get()
    val productionBannerId = providers.gradleProperty("ADMOB_BANNER_UNIT_ID").orElse("").get()

    signingConfigs {
        create("release") {
            storeFile = releaseStorePath.orNull?.takeIf { it.isNotBlank() }?.let { file(it) }
            storePassword = providers.gradleProperty("RELEASE_STORE_PASSWORD").orNull
            keyAlias = providers.gradleProperty("RELEASE_KEY_ALIAS").orElse("aerocalc_upload").get()
            keyPassword = providers.gradleProperty("RELEASE_KEY_PASSWORD").orNull
        }
    }

    buildTypes {
        debug {
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField("String", "ADMOB_APP_ID", "\"ca-app-pub-3940256099942544~3347511713\"")
            buildConfigField("String", "ADMOB_BANNER_UNIT_ID", "\"ca-app-pub-3940256099942544/9214589741\"")
        }
        release {
            manifestPlaceholders["admobAppId"] = productionAppId
            buildConfigField("String", "ADMOB_APP_ID", "\"$productionAppId\"")
            buildConfigField("String", "ADMOB_BANNER_UNIT_ID", "\"$productionBannerId\"")
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

tasks.register("verifyReleaseConfiguration") {
    inputs.property("appId", providers.gradleProperty("ADMOB_APP_ID").orElse(""))
    inputs.property("bannerId", providers.gradleProperty("ADMOB_BANNER_UNIT_ID").orElse(""))
    inputs.property("hasStorePassword", providers.gradleProperty("RELEASE_STORE_PASSWORD").map { it.isNotBlank() }.orElse(false))
    inputs.property("hasKeyPassword", providers.gradleProperty("RELEASE_KEY_PASSWORD").map { it.isNotBlank() }.orElse(false))
    inputs.property("hasKeystore", releaseStorePath.map { it.isNotBlank() && file(it).isFile })
    doLast {
        val appId = inputs.properties.getValue("appId").toString()
        val bannerId = inputs.properties.getValue("bannerId").toString()
        require(Regex("ca-app-pub-\\d{16}~\\d{10}").matches(appId) &&
            appId != "ca-app-pub-3940256099942544~3347511713") {
            "Set a real ADMOB_APP_ID Gradle property before release."
        }
        require(Regex("ca-app-pub-\\d{16}/\\d{10}").matches(bannerId) &&
            bannerId != "ca-app-pub-3940256099942544/9214589741") {
            "Set a real ADMOB_BANNER_UNIT_ID Gradle property before release."
        }
        require(inputs.properties["hasKeystore"] == true &&
            inputs.properties["hasStorePassword"] == true &&
            inputs.properties["hasKeyPassword"] == true) {
            "Release signing is missing. Configure RELEASE_STORE_FILE, RELEASE_STORE_PASSWORD and RELEASE_KEY_PASSWORD outside the repository."
        }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn("verifyReleaseConfiguration") }

kotlin {
    jvmToolchain(17)
}

dependencies {
  implementation("com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:1.5.0")
  implementation("com.google.android.ump:user-messaging-platform:4.0.0")
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.leakcanary.android)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Module dependencies
  implementation(project(":core-domain"))
  implementation(project(":core-math-engine"))
  implementation(project(":core-ui"))
  implementation(project(":core-data"))
  implementation(project(":feature-calculator"))
  implementation(project(":feature-geometry"))
  implementation(project(":feature-physics"))
  implementation(project(":feature-history"))
  implementation(project(":feature-graphing"))
  implementation(project(":feature-chemistry"))
  implementation(project(":feature-converter"))
}
