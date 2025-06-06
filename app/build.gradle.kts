plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.voicecalculator"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.voicecalculator"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.6.5" // must match Compose version
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
    implementation ("androidx.compose.material3:material3:1.1.0")

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
    // ✅ Manually added library for math evaluation
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("net.objecthunter:exp4j:0.4.8")
    implementation("androidx.compose.material3:material3:1.2.0") // or latest version
    implementation ("androidx.compose.material3:material3:1.2.1") // Or latest version
    implementation ("com.google.android.material:material:1.11.0")
    dependencies {
        // Jetpack Compose core dependencies (adjust versions as needed)
        implementation ("androidx.compose.ui:ui:1.5.0")
        implementation ("androidx.compose.ui:ui-tooling-preview:1.5.0")

        // Material3 Design components
        implementation ("androidx.compose.material3:material3:1.1.0")

        // Material Icons Extended (includes Icons.Default.History)
        implementation ("androidx.compose.material:material-icons-extended:1.5.0")

        // Navigation Compose for navigation components
        implementation ("androidx.navigation:navigation-compose:2.7.2")

        // Compose runtime & tooling (optional, but useful)
        implementation ("androidx.compose.runtime:runtime:1.5.0")
        implementation ("androidx.compose.ui:ui-tooling:1.5.0")
        implementation ("androidx.compose.material3:material3:1.2.1")
        implementation("androidx.compose.ui:ui:1.6.5")
    }

}