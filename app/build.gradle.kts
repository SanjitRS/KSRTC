plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "net.kibotu.geofencerelay"
    compileSdk = 35

    defaultConfig {
        applicationId = "net.kibotu.geofencerelay"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["appName"] = "Smaran"
    }

    flavorDimensions += "mode"
    productFlavors {
        create("tracker") {
            dimension = "mode"
            applicationIdSuffix = ".tracker"
            versionNameSuffix = "-tracker"
            manifestPlaceholders["appName"] = "Smaran"
        }
        create("guardian") {
            dimension = "mode"
            applicationIdSuffix = ".guardian"
            versionNameSuffix = "-guardian"
            manifestPlaceholders["appName"] = "Smaran Guardian"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    testImplementation("junit:junit:4.13.2")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Location & Geofencing
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Google Sign-In (Official Google Play Services)
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    // OpenStreetMap (100% Free, zero billing, zero API keys)
    implementation("org.osmdroid:osmdroid-android:6.1.20")

    // Real-time MQTT Relay (Public HiveMQ Broker, 100% Free, zero setup)
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")

    // JSON Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
