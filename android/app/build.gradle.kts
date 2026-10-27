plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
}

android {
  namespace = "io.github.gustavommcv.ciscsmarthome"
  // The current Compose needs to be compiled against Android 17 (API 37). The app itself still
  // targets 36, the Android of the phone, so Android 17's behavior changes do not apply yet.
  compileSdk = 37

  defaultConfig {
    applicationId = "io.github.gustavommcv.ciscsmarthome"
    // 29 is the first Android that lets an app join a Wi-Fi network without internet
    // by itself (WifiNetworkSpecifier), which is how the app reaches the board.
    minSdk = 29
    targetSdk = 36
    versionCode = 1
    versionName = "0.1.0"
  }

  buildTypes {
    release {
      // A demonstration app: the size and the readable stack traces matter more than shrinking.
      isMinifyEnabled = false
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    aidl = false
    buildConfig = false
    shaders = false
  }
  lint {
    // The app targets 36 on purpose: it is the Android of the phone it is tested on. Moving to the
    // latest should come with a test on an Android 17 device.
    disable += "OldTargetApi"
    // allowBackup="false" already turns backup off on every Android version.
    disable += "DataExtractionRules"
  }
  testOptions {
    // Robolectric needs them to start the Android that the local tests of the database run on.
    unitTests.isIncludeAndroidResources = true
  }
  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

kotlin {
  jvmToolchain(17)
}

ksp {
  // Room writes the shape of the database here at each build; it is kept in Git, so a change of the
  // shape shows in review and a later version can be migrated from it.
  arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.okhttp)
  implementation(libs.okhttp.sse)
  implementation(libs.androidx.room.runtime)
  ksp(libs.androidx.room.compiler)
  debugImplementation(libs.androidx.compose.ui.tooling)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.okhttp.mockwebserver)
  testImplementation(libs.robolectric)
  testImplementation(libs.androidx.test.core)
}
