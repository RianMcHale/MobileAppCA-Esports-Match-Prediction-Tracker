import java.util.Properties

// applies the android app plugin listed in the version catalog.
plugins {
    alias(libs.plugins.android.application)
}

// reads local values from .env if that file exists.
val localEnv = Properties().apply {
    val envFile = rootProject.file(".env")
    if (envFile.exists()) {
        envFile.inputStream().use { load(it) }
    }
}

// uses an empty value when no pandascore token was supplied.
val pandaScoreToken = localEnv.getProperty("PANDASCORE_TOKEN", "")

// escapes slashes and quotes so the token fits inside a generated kotlin string.
val pandaScoreTokenLiteral =
    "\"${pandaScoreToken.replace("\\", "\\\\").replace("\"", "\\\"")}\""

android {
    // names the app's generated kotlin and resource package.
    namespace = "org.setu.esportstracker"

    // sets the android api used when compiling.
    compileSdk {
        version = release(37)
    }

    // allows buildconfig to contain the api token value.
    buildFeatures {
        buildConfig = true
    }

    // sets the installed app id and supported android versions.
    defaultConfig {
        applicationId = "org.setu.esportstracker"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        // names the runner for tests on an android device.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        // debug builds receive the local pandascore token.
        debug {
            buildConfigField("String", "PANDASCORE_TOKEN", pandaScoreTokenLiteral)
        }

        // release builds currently have no pandascore token.
        release {
            buildConfigField("String", "PANDASCORE_TOKEN", "\"\"")

            // keeps the named dependency packages during optimisation.
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }

    // compiles the app using java 11 language support.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// adds the libraries used by screens, lists, material controls, and tests.
dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    implementation(libs.androidx.recyclerview)
}