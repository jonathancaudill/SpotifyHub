plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.spotifyhub"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.spotifyhub"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        val spotifyClientId = providers.gradleProperty("SPOTIFY_CLIENT_ID").orElse("").get()
        buildConfigField("String", "SPOTIFY_CLIENT_ID", "\"$spotifyClientId\"")

        val sheetsScriptUrl = providers.gradleProperty("SHEETS_SCRIPT_URL").orElse("").get()
        buildConfigField("String", "SHEETS_SCRIPT_URL", "\"$sheetsScriptUrl\"")

        val spotifyHomeDiscoverReleaseIds = providers.gradleProperty("SPOTIFY_HOME_DISCOVER_RELEASE_IDS").orElse("").get()
        buildConfigField("String", "SPOTIFY_HOME_DISCOVER_RELEASE_IDS", "\"$spotifyHomeDiscoverReleaseIds\"")

        val spotifyHomeDailyMixIds = providers.gradleProperty("SPOTIFY_HOME_DAILY_MIX_IDS").orElse("").get()
        buildConfigField("String", "SPOTIFY_HOME_DAILY_MIX_IDS", "\"$spotifyHomeDailyMixIds\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.security.crypto)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.icons)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.cupertino)
    implementation(libs.cupertino.icons.extended)

    implementation(libs.squircle.shape.android) {
        exclude(group = "org.jetbrains.compose.ui", module = "ui")
        exclude(group = "org.jetbrains.compose.ui", module = "ui-util")
        exclude(group = "org.jetbrains.compose.runtime")
        exclude(group = "org.jetbrains.compose.foundation")
        exclude(group = "org.jetbrains.compose.material3")
    }

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.okhttp.logging)
}
