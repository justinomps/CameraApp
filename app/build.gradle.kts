// The plugins block should already exist at the top of your file.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    // Your existing android configuration block
    namespace = "com.example.cameracomposition" // Make sure this matches your package name
    compileSdk = 34 // Or your target SDK

    defaultConfig {
        applicationId = "com.example.cameracomposition" // Make sure this matches
        minSdk = 21
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    // --- THIS IS THE CRITICAL PART ---
    // This block enables the View Binding feature for your project.
    buildFeatures {
        viewBinding = true
    }
    // ---------------------------------
}

dependencies {
    // Standard dependencies that should already be there
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // --- CameraX Dependencies ---
    val cameraxVersion = "1.3.1"
    implementation("androidx.camera:camera-core:${cameraxVersion}")
    implementation("androidx.camera:camera-camera2:${cameraxVersion}")
    implementation("androidx.camera:camera-lifecycle:${cameraxVersion}")
    implementation("androidx.camera:camera-view:${cameraxVersion}")
    implementation("com.google.code.gson:gson:2.9.0")
    // ----------------------------
}
