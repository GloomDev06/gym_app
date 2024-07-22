plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.fitness.myhealthfitness"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.fitness.myhealthfitness"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "2.1"

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.firebase.storage.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

//    FOR QR CODE
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")


//    EMAIL - PASSWORDLESS SIGN IN
    implementation(platform("com.google.firebase:firebase-bom:33.1.1"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.android.gms:play-services-auth:21.2.0")


//    LOTTIE FILES
    implementation("com.airbnb.android:lottie:3.4.0")

//    RAZRO PAY
    implementation("com.razorpay:checkout:1.6.38")

//    JUSTIFY TEXT VIEW
    implementation("com.codesgood:justifiedtextview:1.1.0")

//    PICASSO
    implementation("com.squareup.picasso:picasso:2.71828")


//    GLIDE
    implementation("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")

    // Rounded Image View
    implementation ("de.hdodenhof:circleimageview:3.1.0")
}