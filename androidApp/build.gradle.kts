plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.compose.compiler)
    id("com.google.devtools.ksp") version "2.0.21-1.0.27"
}

android {
    namespace = "com.jsramraj.playmatecompanion.android"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.jsramraj.playmatecompanion.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(projects.shared)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.compose.material:material-icons-core:1.5.1")
    implementation("androidx.compose.material:material-icons-extended:1.5.1")
    implementation(libs.google.auth)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.navigation.compose)
    
    // Accompanist (for SwipeRefresh)
    implementation(libs.accompanist.swiperefresh)
    
    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    ksp("androidx.room:room-compiler:2.5.0")
    implementation(libs.gson)
    debugImplementation(libs.compose.ui.tooling)
}