plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.mini"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.mini"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    
    // Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    // kapt("androidx.room:room-compiler:$roomVersion") // Will fix later if needed
    // Note: Since this is a Kotlin project, we should use ksp, but for simplicity we will add ksp plugin in libs.versions.toml or build.gradle.kts.
    // Wait, let's use kapt for simplicity if ksp is not set up.
    // Actually, KSP is recommended, but let's try to add it. Let's modify the top plugins block too.
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Java Diff Utils
    implementation("io.github.java-diff-utils:java-diff-utils:4.12")
    
    // Markwon (pulls com.atlassian.commonmark:0.13.0 transitively - same org.commonmark.* classes)
    implementation("io.noties.markwon:core:4.6.2")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}