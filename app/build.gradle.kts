plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlinx-serialization")
    id("kotlin-kapt")
}

android {
    namespace = "com.example.bismillahsipfo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.bismillahsipfo"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "BASE_URL", "\"https://ulxdrgkjbvalhxesibpr.supabase.co\"")
        buildConfigField("String", "API_KEY", "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVseGRyZ2tqYnZhbGh4ZXNpYnByIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Mzk3MTgwOTUsImV4cCI6MjA1NTI5NDA5NX0.r7cDt4eJHFHELtNneP6_Q8SNl_Eg8Vj3GzVOIr9Pmr8\"")
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
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.androidx.recyclerview)
    implementation(libs.glide)
    kapt ("com.github.bumptech.glide:compiler:4.16.0")
    implementation(libs.material)
    implementation(libs.androidx.viewpager2)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")

    //room
    implementation("androidx.room:room-runtime:2.6.0")
    annotationProcessor ("androidx.room:room-compiler:2.3.0")
    kapt("androidx.room:room-compiler:2.6.0")

    // supabase
    implementation(platform("io.github.jan-tennert.supabase:bom:3.1.2"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt:3.1.2")
    implementation("io.github.jan-tennert.supabase:storage-kt:3.1.2")
//    implementation("io.github.jan-tennert.supabase:realtime-kt")
    implementation("io.ktor:ktor-client-android:3.1.1")


}