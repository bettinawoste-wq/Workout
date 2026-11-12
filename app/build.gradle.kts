plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.room)
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

    // MigrationTestHelper reads the exported schemas (see room {} below) from the test APK.
    sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// One JSON file per database version, committed, so MigrationTestHelper can open old
// versions in migration tests (ADR 0004).
room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.arch.core.testing)
    androidTestImplementation(libs.room.testing)
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

    constraints {
        // The test APK must use the app's library versions, and the app gets 1.7.3 through
        // navigation. room-testing reads the exported schemas with kotlinx-serialization-json
        // 1.8.1, which fails at runtime (AbstractMethodError) against an older core.
        implementation(libs.kotlinx.serialization.core) {
            because("room-testing's MigrationTestHelper needs serialization-core 1.8.1")
        }
    }
}