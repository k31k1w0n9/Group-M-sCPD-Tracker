plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

fun prop(name: String, default: String): String = providers.gradleProperty(name).getOrElse(default)

/**
 * The real project's IDs come from ~/.gradle/gradle.properties (scpd.firebase.live.projectId, .appId, .apiKey), never from
 * the repo. See firebase/README.md. Without them a "live" build fails here, so a release can't ship pointing at nothing.
 */
fun com.android.build.api.dsl.VariantDimension.firebaseFields(live: Boolean) {
    fun liveProp(key: String): String = providers.gradleProperty("scpd.firebase.live.$key").orNull
        ?: if (gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) || live && it.contains("Debug") })
            throw GradleException("Set scpd.firebase.live.$key in ~/.gradle/gradle.properties (see firebase/README.md).")
        else ""
    val projectId = if (live) liveProp("projectId") else "demo-scpd"
    val appId = if (live) liveProp("appId") else "1:000000000000:android:0000000000000000"
    val apiKey = if (live) liveProp("apiKey") else "demo-api-key"
    buildConfigField("String", "FIREBASE_PROJECT_ID", "\"$projectId\"")
    buildConfigField("String", "FIREBASE_APP_ID", "\"$appId\"")
    buildConfigField("String", "FIREBASE_API_KEY", "\"$apiKey\"")
    buildConfigField("String", "FIREBASE_EMULATOR_HOST", "\"${if (live) "" else prop("scpd.firebase.emulatorHost", "127.0.0.1")}\"")
}

android {
    namespace = "com.example.scpd_tracker"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.scpd_tracker"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Debug builds: "fake" (in memory, the default, used by every test) or "firebase" (-Pscpd.backend=firebase),
        // which talks to the free local emulators as the demo project demo-scpd.
        // 127.0.0.1 on the phone is tunnelled to the laptop with `adb reverse tcp:9099 tcp:9099` and `adb reverse tcp:8080 tcp:8080`.
        // Add -Pscpd.firebase.target=live to point a debug build at the real project instead (for example to test it on the Pixel 2).
        val live = prop("scpd.firebase.target", "emulator") == "live"
        buildConfigField("String", "BACKEND", "\"${if (live) "firebase" else prop("scpd.backend", "fake")}\"")
        firebaseFields(live)
    }

    buildTypes {
        release {
            // The release APK (what the markers install) always uses the real Firebase project and never the emulators.
            buildConfigField("String", "BACKEND", "\"firebase\"")
            firebaseFields(live = true)
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // java.time (ZonedDateTime, Duration) is only built in from API 26; desugaring covers minSdk 24.
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.zxing.core)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
