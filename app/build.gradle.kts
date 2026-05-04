plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // Hilt
    alias(libs.plugins.hilt)
    // KSP
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.kiniot.uflex"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.kiniot.uflex"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    // Navigation Compose
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    // Material Icons
    implementation(libs.androidx.compose.material.icons.extended)
    // KSP
    ksp(libs.hilt.compiler)
    ksp(libs.moshi.kotlin.codegen)
    ksp(libs.room.compiler)
    ksp(libs.androidx.hilt.compiler)
    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    // OkHttp
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    // Moshi
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    // DataStore
    implementation(libs.datastore)
    // Coroutines
    implementation(libs.coroutines)
    // Hilt Common
    implementation(libs.androidx.hilt.common)
    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)
    // Hilt + WorkManager
    implementation(libs.androidx.hilt.work)
    // Compose UI Text
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.androidx.compose.ui.text)
}