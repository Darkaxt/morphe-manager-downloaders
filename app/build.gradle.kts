plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

repositories { google(); mavenCentral() }
providers.gradleProperty("buildRoot").orNull?.let {
    layout.buildDirectory.set(file("$it/companion"))
}
android {
    namespace = "app.morphe.manager.downloaders"
    compileSdk = 36
    defaultConfig {
        applicationId = "app.morphe.manager.downloaders"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { buildConfig = true }
    buildTypes {
        release {
            isMinifyEnabled = false
            val keyPath = providers.gradleProperty("companionKeystore").orNull
            if (keyPath != null) {
                signingConfig = signingConfigs.create("companion") {
                    storeFile = file(keyPath)
                    storePassword = System.getenv("COMPANION_KEYSTORE_PASSWORD")
                    keyAlias = "morphe-downloaders"
                    keyPassword = System.getenv("COMPANION_KEYSTORE_PASSWORD")
                }
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin { jvmToolchain(17) }
dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
