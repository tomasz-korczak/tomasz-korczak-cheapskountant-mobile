import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose)
    alias(libs.plugins.serialization)
    alias(libs.plugins.ksp)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun buildConfigString(name: String, default: String): String {
    val value = localProperties.getProperty(name) ?: default
    val escaped = value.replace("\\", "\\\\").replace("\"", "\\\"")
    return "\"$escaped\""
}

android {
    namespace = "pl.cheapskountant.mobile"
    compileSdk = 37

    defaultConfig {
        applicationId = "pl.cheapskountant.mobile"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_BASE_URL", buildConfigString("api.baseUrl", "http://192.168.1.20:8080/"))
        buildConfigField("String", "API_KEY", buildConfigString("api.key", "example-key"))
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

configurations.configureEach {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:2.2.10")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.2.10")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.2.10")
    }
}

dependencies {
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons)
    implementation(libs.compose.icons.extended)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)
    implementation(libs.core.ktx)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.camera.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.work.runtime)
    implementation(libs.serialization.json)
    implementation(libs.coroutines.android)
    ksp(libs.room.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.okhttp)
    testImplementation(libs.serialization.json)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.room.testing)
    debugImplementation(libs.compose.ui.test.manifest)
}
