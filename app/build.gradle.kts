@file:Suppress("UnstableApiUsage")

import com.android.build.api.variant.FilterConfiguration.FilterType.ABI
import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import com.github.willir.rust.CargoNdkBuildTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.konan.properties.hasProperty
import org.jetbrains.kotlin.konan.properties.propertyList

val localProperties = gradleLocalProperties(rootDir, providers)
val abiFilterList = ((localProperties["ABI_FILTERS"] ?: properties["ABI_FILTERS"]) as? String)
    ?.split(';')
val abiCodes = mapOf("armeabi-v7a" to 1, "arm64-v8a" to 2, "x86" to 3, "x86_64" to 4)

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.cargoNdkAndroid)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "rs.ruffle"
    compileSdk = 35

    // [新增] 强制使用你本地安装的 NDK 版本
    ndkVersion = "26.1.10909125"
    
    defaultConfig {
        applicationId = "rs.ruffle"
        minSdk = 26
        targetSdk = 35
        versionCode = 260202
        versionName = "0.260202"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            if (abiFilterList == null) {
                abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
            }
        }
    }

    // === 【修改点 1】配置本地签名 ===
    signingConfigs {
        create("release") {
            // 指向项目根目录下的 my-release-key.jks
            storeFile = file("${project.rootDir}/my-release-key.jks")
            storePassword = "123456"
            keyAlias = "my-key-alias"
            keyPassword = "123456"
        }
    }

    buildTypes {
        release {
            // === 【修改点 2】开启混淆和资源压缩以减小体积 ===
            isMinifyEnabled = false
            isShrinkResources = false // 移除未使用的资源文件
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 应用上面定义的签名配置
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        compose = true
        prefab = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    splits {
        // Configures multiple APKs based on ABI.
        abi {
            // Enables building multiple APKs per ABI.
            isEnable = true

            // Resets the list of ABIs that Gradle should create APKs for to none.
            reset()

            // Specifies a list of ABIs that Gradle should create APKs for.
            if (abiFilterList != null && abiFilterList.isNotEmpty()) {
                include(*abiFilterList.toTypedArray())
            } else {
                include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")

                // Specifies that we also want to generate a universal APK that includes all ABIs.
                isUniversalApk = true
            }
        }
    }
    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val name = output.filters.find { it.filterType == ABI }?.identifier
            val abiCode = abiCodes[name] ?: 0
            output.versionCode.set(output.versionCode.get() * 10 + abiCode)
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.games.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.appcompat)
    implementation(libs.gson) // 新增
    implementation("androidx.documentfile:documentfile:1.0.1") // [阶段二新增] 用于文件夹操作
    implementation("androidx.compose.material:material-icons-extended:1.6.8")
    implementation("com.google.code.gson:gson:2.10.1")
    androidTestImplementation(libs.androidx.uiautomator)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// On GHA, we prebuild the native libs separately for fasterness,
// and this plugin doesn't recognize them, so would build them again.
if (System.getenv("GITHUB_ACTIONS") != null) {
    tasks.withType<CargoNdkBuildTask> {
        enabled = false
    }
}

cargoNdk {
    module = "."
    apiLevel = 26
    buildType = "release"

    if (localProperties.hasProperty("ndkTargets")) {
        targets = ArrayList(localProperties.propertyList("ndkTargets"))
    }
}
