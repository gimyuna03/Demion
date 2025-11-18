plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.iot.medion"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.iot.medion"
        minSdk = 26
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // TensorFlow Lite 관련 의존성 추가
    implementation("org.tensorflow:tensorflow-lite:2.15.0")
// 모델 실행을 위한 핵심 라이브러리
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
// 모델 입력/출력 처리 편리하게 해주는 라이브러리
    implementation("org.tensorflow:tensorflow-lite-metadata:0.4.4")
// 모델 메타데이터 처리 (필요시)
// 이미지 로드 및 처리 라이브러리 (Glide 또는 Coil 사용 권장)
// 예를 들어 Glide를 사용한다면:
// implementation 'com.github.bumptech.glide:glide:4.16.0'
// annotationProcessor 'com.github.bumptech.glide:compiler:4.16.0'
}