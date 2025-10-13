// app/build.gradle.kts
plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.infowave.thedoctorathomeuser"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.infowave.thedoctorathomeuser"
        minSdk = 24   // ⬅️ Firebase Auth ≥ 23.2.x को सपोर्ट करने के लिए 21 से बढ़ाकर 23 करें
        targetSdk = 35
        versionCode = 6
        versionName = "5.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // signingConfig = signingConfigs.getByName("release") // Optional: release keystore set करें
        }
        getByName("debug") {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
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
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.drawerlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.google.android.gms:play-services-maps:18.1.0")

    // ✅ Firebase (single BOM)
    implementation(platform("com.google.firebase:firebase-bom:33.14.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-analytics")

    implementation("com.android.volley:volley:1.2.1")
    implementation("androidx.cardview:cardview:1.0.0")

    implementation("com.razorpay:checkout:1.6.26")
    implementation("com.airbnb.android:lottie:3.7.0")
    implementation("com.cashfree.pg:api:2.1.25")

    // ✅ Glide (latest + matching compiler)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.google.android.gms:play-services-location:21.0.1")


    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("phonepe.intentsdk.android.release:IntentSDK:5.1.1")


    implementation("pl.droidsonroids.gif:android-gif-drawable:1.2.23")
}
