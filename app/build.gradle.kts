plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// La versión se puede inyectar desde CI: -PversionName=0.2.0 -PversionCode=200
val appVersionName: String = (project.findProperty("versionName") as String?) ?: "0.1.0"
val appVersionCode: Int = (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: 1

android {
    namespace = "com.kherio.khome"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.kherio.khome"
        minSdk = 24
        targetSdk = 34
        versionCode = appVersionCode
        versionName = appVersionName

        // Clave de TMDB para las novedades. En CI llega del secreto TMDB_API_KEY; si falta, no hay filas.
        val tmdbKey = System.getenv("TMDB_API_KEY") ?: (project.findProperty("tmdbKey") as String?) ?: ""
        buildConfigField("String", "TMDB_KEY", "\"$tmdbKey\"")
    }

    // Firma estable: así cada APK nuevo se instala "encima" del anterior (actualización).
    // En CI se puede usar tu propia keystore con las variables de entorno KEYSTORE_PATH,
    // KEYSTORE_PASSWORD, KEY_ALIAS y KEY_PASSWORD; si no existen, se usa app/debug.keystore.
    signingConfigs {
        create("khome") {
            val keystorePath = System.getenv("KEYSTORE_PATH")
            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            } else {
                storeFile = file("debug.keystore")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("khome")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("khome")
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
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.tv:tv-material:1.0.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
