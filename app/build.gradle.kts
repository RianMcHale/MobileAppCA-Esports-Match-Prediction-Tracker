import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val localEnv = Properties().apply {
    val envFile = rootProject.file(".env")
    if (envFile.exists()) {
        envFile.inputStream().use { load(it) }
    }
}
val pandaScoreToken = localEnv.getProperty("PANDASCORE_TOKEN", "")
val pandaScoreTokenLiteral =
    "\"${pandaScoreToken.replace("\\", "\\\\").replace("\"", "\\\"")}\""

android {
    namespace = "org.setu.esportstracker"
    compileSdk {
        version = release(37)
    }

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "org.setu.esportstracker"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "PANDASCORE_TOKEN", pandaScoreTokenLiteral)
        }
        release {
            buildConfigField("String", "PANDASCORE_TOKEN", "\"\"")
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }

    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

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