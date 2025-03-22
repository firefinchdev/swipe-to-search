plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.firefinchdev.swipetosearch"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.firefinchdev.swipetosearch"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
       create("release") {
           val keystoreFile = rootProject.file("app/keystore.jks")
           if (keystoreFile.exists()) {
               storeFile = keystoreFile
               storePassword = System.getenv("SIGNING_STORE_PASSWORD") ?: findProperty("SIGNING_STORE_PASSWORD") as String? ?: ""
               keyAlias = System.getenv("SIGNING_KEY_ALIAS") ?: findProperty("SIGNING_KEY_ALIAS") as String? ?: ""
               keyPassword = System.getenv("SIGNING_KEY_PASSWORD") ?: findProperty("SIGNING_KEY_PASSWORD") as String? ?: ""
           }
       }
   }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
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

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}