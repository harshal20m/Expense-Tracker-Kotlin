import java.io.FileInputStream
import java.util.Properties

// Load keystore properties
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.paisatracker"
    compileSdk = 36

    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties["storeFile"] as String? ?: "")
            storePassword = keystoreProperties["storePassword"] as String? ?: ""
            keyAlias = keystoreProperties["keyAlias"] as String? ?: ""
            keyPassword = keystoreProperties["keyPassword"] as String? ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    defaultConfig {
        applicationId = "com.example.paisatracker"
        minSdk = 28
        targetSdk = 36
        versionCode = 2
        versionName = "v2.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Core AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.fragment:fragment-ktx:1.8.6")

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Room Database - Updated to 2.7.0-alpha13 to support Kotlin 2.0+
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.androidx.foundation.layout)
    ksp(libs.room.compiler)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)

    // UI & Charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Utilities & Background Work
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("androidx.datastore:datastore-preferences:1.1.2")
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    implementation("com.opencsv:opencsv:5.9")

    // PDF Generation
    implementation("com.itextpdf:itext7-core:7.2.6")
    implementation("com.itextpdf:kernel:7.2.6")
    implementation("com.itextpdf:layout:7.2.6")
    implementation("com.itextpdf:io:7.2.6")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // Glance Widgets
    implementation("androidx.glance:glance:1.1.1")
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")
}