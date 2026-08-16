plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.hebbar.litelauncher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hebbar.litelauncher"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystoreFile = rootProject.file("release.keystore")
            if (!keystoreFile.exists()) {
                try {
                    val process = ProcessBuilder(
                        "keytool", "-genkeypair", "-v",
                        "-keystore", keystoreFile.absolutePath,
                        "-alias", "litelauncher",
                        "-keyalg", "RSA",
                        "-keysize", "2048",
                        "-validity", "10000",
                        "-storepass", "litelauncher123",
                        "-keypass", "litelauncher123",
                        "-dname", "CN=LiteLauncher, OU=Mobile, O=LiteLauncher, L=Bangalore, ST=Karnataka, C=IN"
                    ).start()
                    process.waitFor()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = "litelauncher123"
                keyAlias = "litelauncher"
                keyPassword = "litelauncher123"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            val releaseSigning = signingConfigs.findByName("release")
            signingConfig = if (releaseSigning?.storeFile?.exists() == true) releaseSigning else signingConfigs.getByName("debug")
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

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}