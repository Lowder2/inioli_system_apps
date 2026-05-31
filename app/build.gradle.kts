import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun propertyOrDefault(name: String, defaultValue: String): String {
    return (localProperties.getProperty(name)
        ?: providers.gradleProperty(name).orNull
        ?: defaultValue)
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
}

android {
    namespace = "com.lowderancorp.inioli"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.lowderancorp.inioli"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildConfigField(
            "String",
            "LOGIN_BASE_URL",
            "\"${propertyOrDefault("INIOLI_LOGIN_BASE_URL", "https://lowderancorp.com/inioli/server/api/")}\""
        )
        buildConfigField(
            "String",
            "LOGIN_AUTHORIZATION_HEADER",
            "\"Bearer ${propertyOrDefault("INIOLI_LOGIN_BEARER_TOKEN", "416cdab68e21320efe462bf9b8d6a4ca1219af79e29b8d0ed1b6cc259d2398da")}\""
        )
        buildConfigField(
            "String",
            "STOCK_MOVEMENT_BASE_URL",
            "\"${propertyOrDefault("INIOLI_STOCK_MOVEMENT_BASE_URL", "https://lowderancorp.com/inioli/server/api/")}\""
        )

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
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
