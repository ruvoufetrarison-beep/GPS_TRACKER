plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.fanofeno.gpstracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.fanofeno.gpstracker"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField("String", "SUPABASE_URL", "\"https://urvxpgjefdmfteldecah.supabase.co\"")
        buildConfigField("String", "SUPABASE_KEY", "\"sb_publishable_5nL6WkK7QF5cRyxmCL4pyw_zSPbP0Ct\"")
    }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
}
