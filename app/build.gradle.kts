plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// Echo (remote access) needs Firebase. The plugin is only applied when a real
// google-services.json is present, so the project builds out of the box without one.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

// Optional release signing, used by the GitHub Actions workflow and by local builds
// that export EXTENDROID_KEYSTORE.
val ciKeystore: File? = System.getenv("EXTENDROID_KEYSTORE")
    ?.takeIf { it.isNotBlank() }
    ?.let { file(it) }
    ?.takeIf { it.exists() }

android {
    namespace = "dev.legendsayantan.extendroid"
    compileSdk = 35

    buildFeatures {
        aidl = true
    }

    lint {
        baseline = file("lint-baseline.xml")
    }

    defaultConfig {
        applicationId = "dev.legendsayantan.extendroid"
        minSdk = 29
        targetSdk = 33
        versionCode = 7
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("ci") {
            if (ciKeystore != null) {
                storeFile = ciKeystore
                storePassword = System.getenv("EXTENDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("EXTENDROID_KEY_ALIAS") ?: "extendroid"
                keyPassword = System.getenv("EXTENDROID_KEY_PASSWORD")
                    ?: System.getenv("EXTENDROID_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (ciKeystore != null) {
                signingConfig = signingConfigs.getByName("ci")
            }
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
    sourceSets {
        getByName("main") {
            aidl.srcDir("aidl")  // ensure AIDL files are in this folder
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.shizuku)
    implementation(libs.shizukuProvider)
    implementation(libs.hiddenApi)
    implementation(libs.palette) // Added Palette library
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.appcheck.playintegrity)
    //implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.stream.webrtc.android)
    // Gson for JSON parsing
    implementation(libs.gson)


        testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
