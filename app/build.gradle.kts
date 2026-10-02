plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.parkguardai"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.parkguardai"
        minSdk = 29
        targetSdk = 36
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

    buildFeatures {
        viewBinding = true
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

    // Hỗ trợ điều hướng giữa các Fragment
    implementation(libs.navigation.fragment)
    // Kết nối Navigation với các thành phần UI (Toolbar, BottomNav,...)
    implementation(libs.navigation.ui)

    // Tải và thực thi mô hình YOLO11n định dạng TFLite.
    implementation(libs.tensorflow.lite)

    // Xác thực email/mật khẩu và gửi liên kết xác minh email trên Firebase Spark.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
