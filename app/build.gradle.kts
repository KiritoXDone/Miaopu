plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val releaseKeystorePath = providers.environmentVariable("MIAOPU_KEYSTORE_PATH").orNull
val releaseKeystorePassword = providers.environmentVariable("MIAOPU_KEYSTORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("MIAOPU_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("MIAOPU_KEY_PASSWORD").orNull
val releaseSigningValues = listOf(
    releaseKeystorePath,
    releaseKeystorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
)
val hasReleaseSigning = releaseSigningValues.all { !it.isNullOrBlank() }
check(releaseSigningValues.none { !it.isNullOrBlank() } || hasReleaseSigning) {
    "Release signing environment variables must be provided together."
}

android {
    namespace = "dev.kiritoxd.miaopu"
    buildToolsVersion = "37.0.0"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        applicationId = "dev.kiritoxd.miaopu"
        minSdk = 24
        targetSdk = 37
        versionCode = 48
        versionName = "2.4.1"
        manifestPlaceholders["appLabel"] = "喵扑"

        testInstrumentationRunner = "dev.kiritoxd.miaopu.widget.WidgetTestRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(checkNotNull(releaseKeystorePath))
                storePassword = checkNotNull(releaseKeystorePassword)
                keyAlias = checkNotNull(releaseKeyAlias)
                keyPassword = checkNotNull(releaseKeyPassword)
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            manifestPlaceholders["appLabel"] = "喵扑测试版"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildTypes.create("releaseTest") {
        initWith(buildTypes.getByName("release"))
        applicationIdSuffix = ".releaseTest"
        versionNameSuffix = "-release-test"
        manifestPlaceholders["appLabel"] = "喵扑 Release 测试"
        signingConfig = signingConfigs.getByName("debug")
        matchingFallbacks += "release"
    }

    packaging {
        jniLibs {
            excludes += "lib/*/libandroidx.graphics.path.so"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
    }
}

dependencies {
    // Resolved to the submodule in settings.gradle.kts, not GitHub Packages.
    val miuixVersion = "0.9.4-2855a36b-SNAPSHOT"
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.navigationevent:navigationevent-compose:1.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.13.0")
    implementation("top.yukonga.miuix.kmp:miuix-ui:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-nav:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-glass:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-glass-icons:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-preference:$miuixVersion")
    implementation("io.coil-kt.coil3:coil-compose:3.5.0")
    implementation("io.coil-kt.coil3:coil-gif:3.5.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.5.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20250517")
}
