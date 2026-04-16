import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrains.kotlin.kapt)
    id("com.google.gms.google-services")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}
val azureIotConnectionString = localProperties.getProperty("azureIotConnectionString") ?: ""
val azureIotHostName = localProperties.getProperty("HostName") ?: ""

android {
    namespace = "com.example.tesisv3"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.tesisv3"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "AZURE_IOT_CONNECTION_STRING",
            "\"${azureIotConnectionString}\""
        )
        buildConfigField(
            "String",
            "AZURE_IOT_HOST_NAME",
            "\"${azureIotHostName}\""
        )
    }
    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            enableUnitTestCoverage = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
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
    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }

    packaging {
        jniLibs {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/io.netty.versions.properties"
            excludes += "/META-INF/LICENSE.md"
            pickFirsts += "/META-INF/LICENSE.md"
            excludes += "/META-INF/NOTICE.md"
            pickFirsts += "/META-INF/NOTICE.md"
        }
    }
}
dependencies {
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    implementation("org.conscrypt:conscrypt-android:2.5.2")
    implementation("com.google.android.gms:play-services-base:18.5.0")
    implementation(platform("com.google.firebase:firebase-bom:34.11.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-analytics")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.room:room-runtime:2.7.0")
    implementation("androidx.room:room-ktx:2.7.0")
    kapt("androidx.room:room-compiler:2.7.0")
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.legacy.legacysupport)
    implementation(libs.jetbrains.kotlinx.couroutine)
    implementation(libs.coil.kt)
    implementation(libs.jetbrains.kotlin.stdlib)
    implementation(libs.google.android.material)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    implementation(libs.glide)
    kapt(libs.glide.compiler)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.windowsize)
    implementation(libs.androidx.compose.material3.windowsize)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.core.splashscreen)
    implementation(libs.androidx.compose.material.material.icons)
    implementation(libs.google.accompanist.accompanist)
    implementation(libs.androidx.biometric)
    implementation(libs.playservices.wearable)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.core.ktx)
}
