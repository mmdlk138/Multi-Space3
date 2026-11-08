
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
}

val keystoreProps = rootProject.file("keystore.properties")

android {
    namespace = "app.multinest"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.multinest.browser"
        minSdk = 26
        targetSdk = 34
        versionCode = 4
        versionName = "1.2.0-gecko-test"
    }

    signingConfigs {
        if (keystoreProps.exists()) {
            create("release") {
                val p = Properties().apply {
                    keystoreProps.inputStream().use { load(it) }
                }
                storeFile = rootProject.file(p.getProperty("storeFile"))
                storePassword = p.getProperty("storePassword")
                keyAlias = p.getProperty("keyAlias")
                keyPassword = p.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".gecko"
            versionNameSuffix = "-gecko"
        }

        release {
            isMinifyEnabled = false
            if (keystoreProps.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
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
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")

    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.core:core-ktx:1.13.1")

    implementation("org.mozilla.geckoview:geckoview:153.0.20260810162159")

    val room = "2.6.1"

    implementation("androidx.room:room-runtime:$room")
    implementation("androidx.room:room-ktx:$room")

    ksp("androidx.room:room-compiler:$room")

    testImplementation("junit:junit:4.13.2")
}
