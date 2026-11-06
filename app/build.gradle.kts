plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.wob.workout"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.wob.workout"
        minSdk = 24
        targetSdk = 37
        // The release workflow passes -PversionName / -PversionCode; local builds keep 1 / "1.0".
        versionCode = providers.gradleProperty("versionCode").map(String::toInt).getOrElse(1)
        versionName = providers.gradleProperty("versionName").getOrElse("1.0")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Release signing comes from the environment (the release workflow sets these from GitHub
    // Secrets); the keystore itself is never committed. Without KEYSTORE_PATH - a local build -
    // the release variant falls back to the debug key so it can still be installed.
    val keystorePath: String? = System.getenv("KEYSTORE_PATH")
    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // Installs next to a release build as its own app with its own database.
            applicationIdSuffix = ".debug"
        }
        release {
            signingConfig = signingConfigs.getByName(
                if (keystorePath != null) "release" else "debug"
            )
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.arch.core.testing)
    annotationProcessor(libs.room.compiler)
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.recyclerview)
    implementation(libs.room.runtime)
    testImplementation(libs.junit)
}