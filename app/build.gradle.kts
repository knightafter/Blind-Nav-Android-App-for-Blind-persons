plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.example.assistantapp"
    compileSdk = 34



    packaging {
        resources {
            excludes.add("META-INF/INDEX.LIST")
            excludes.add("META-INF/DEPENDENCIES")
        }
    }

    defaultConfig {
        applicationId = "com.example.assistantapp"
        minSdk = 27
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.tracing.perfetto.handshake)
    implementation(libs.androidx.media3.common)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)


    // CameraX dependencies
    implementation("androidx.camera:camera-core:1.3.4")
    implementation ("androidx.camera:camera-camera2:1.3.4")
    implementation ("androidx.camera:camera-lifecycle:1.3.4")
    implementation ("androidx.camera:camera-view:1.3.4")

    // Text-to-Speech (TTS) dependencies
    implementation ("androidx.core:core:1.13.1")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    implementation ("androidx.compose.material:material-icons-core:1.0.1")
    implementation ("androidx.compose.material:material-icons-extended:1.0.1")


    implementation("com.google.cloud:google-cloud-speech:2.2.1")


}