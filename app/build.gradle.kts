plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.infowave.thedoctorathomeuser"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.infowave.thedoctorathomeuser"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }

        getByName("debug") {
            isDebuggable = true
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
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
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation ("de.hdodenhof:circleimageview:3.1.0")
//    implementation ("com.google.android.gms:play-services-maps:18.1.0")


    implementation ("com.google.firebase:firebase-auth:23.2.0")

    implementation ("com.google.firebase:firebase-core:21.1.1")

    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))


    implementation ("com.android.volley:volley:1.2.1")
    implementation ("androidx.cardview:cardview:1.0.0")

    implementation ("com.razorpay:checkout:1.6.26")

    implementation ("com.airbnb.android:lottie:3.7.0")

    implementation ("com.cashfree.pg:api:2.1.25")

    implementation ("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.12.0")

    implementation ("com.google.code.gson:gson:2.10.1")

    implementation ("com.google.android.gms:play-services-location:21.0.1")

    implementation ("com.github.bumptech.glide:glide:4.15.1")
    implementation ("com.github.bumptech.glide:okhttp3-integration:4.15.1")





}
