plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.aboutLibraries)
    id("kotlin-parcelize")
}

android {
    namespace = "me.kavishdevar.librepods"
    compileSdk = 36

    defaultConfig {
        applicationId = "me.kavishdevar.librepods"
        minSdk = 33
        targetSdk = 36

        // CI version injection
        val ciVersionCode = project.findProperty("ciVersionCode")?.toString()?.toIntOrNull()
        val ciVersionName = project.findProperty("ciVersionName")?.toString()

        versionCode = ciVersionCode ?: 9
        versionName = ciVersionName ?: "0.2.0"
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_FILE")
            val keystorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            val keyAliasEnv = System.getenv("ANDROID_KEY_ALIAS")
            val keyPasswordEnv = System.getenv("ANDROID_KEY_PASSWORD")

            if (!keystorePath.isNullOrBlank() &&
                !keystorePassword.isNullOrBlank() &&
                !keyAliasEnv.isNullOrBlank() &&
                !keyPasswordEnv.isNullOrBlank()
            ) {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                keyAlias = keyAliasEnv
                keyPassword = keyPasswordEnv
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true

            // Only attach signing if keystore exists
            if (signingConfigs.getByName("release").storeFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }

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
        compose = true
        viewBinding = true
    }

    androidResources {
        generateLocaleConfig = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    sourceSets {
        getByName("main") {
            res.srcDirs("src/main/res", "src/main/res-apple")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.accompanist.permissions)
    implementation(libs.hiddenapibypass)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.annotations)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.haze)
    implementation(libs.haze.materials)
    implementation(libs.androidx.dynamicanimation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.aboutlibraries)
    implementation(libs.aboutlibraries.compose.m3)

    debugImplementation(libs.androidx.compose.ui.tooling)

    compileOnly(files("libs/libxposed-api-100.aar"))

    debugImplementation(files("libs/backdrop-debug.aar"))
    releaseImplementation(files("libs/backdrop-release.aar"))
}

aboutLibraries {
    export {
        prettyPrint = true
        excludeFields = listOf("generated")
        outputFile = file("src/main/res/raw/aboutlibraries.json")
    }
}
