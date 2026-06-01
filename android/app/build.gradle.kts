import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Config local del backend para desarrollo, leída de local.properties (no versionado).
// `device.api.host`: IP de la PC en la LAN para correr en un celular físico (fallback abajo).
// `api.port`: puerto del backend (default 8080; se override si el 8080 está ocupado, ej: Apache).
val localProps = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) localFile.inputStream().use { load(it) }
}
val deviceApiHost: String = (localProps.getProperty("device.api.host") ?: "192.168.101.75").trim()
val apiPort: String = (localProps.getProperty("api.port") ?: "8080").trim()

android {
    namespace = "com.controlfinanciero"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.controlfinanciero"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    flavorDimensions += "target"
    productFlavors {
        create("emulator") {
            dimension = "target"
            // 10.0.2.2 = alias del localhost de la PC desde el emulador de Android.
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:$apiPort\"")
        }
        create("device") {
            dimension = "target"
            // IP de la PC en la LAN + puerto (configurables en local.properties).
            buildConfigField("String", "API_BASE_URL", "\"http://$deviceApiHost:$apiPort\"")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Activity & Lifecycle
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Retrofit + Serialization
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    // Charts - Vico (Compose-native charts)
    implementation("com.patrykandpatrick.vico:compose-m3:2.0.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // DataStore (persistencia del token de sesión)
    implementation("androidx.datastore:datastore-preferences:1.1.1")
}
