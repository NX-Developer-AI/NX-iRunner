plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.github.willir.rust.cargo-ndk-android")
}

val coreDirectory = rootDir.resolve("../core/touchHLE").canonicalFile
val generatedCoreAssetsDirectory = layout.buildDirectory.dir("generated/touchhle-assets")
val applicationIdentifier = "com.nxteam.irunner"
val providerAuthority = "$applicationIdentifier.provider"

val releaseKeystorePath: String? = System.getenv("NX_KEYSTORE_PATH")

android {
    namespace = applicationIdentifier
    compileSdk = 35
    ndkVersion = "25.2.9519653"

    defaultConfig {
        applicationId = applicationIdentifier
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "APP_NAME", "\"NX iRunner\"")
        buildConfigField("String", "PROVIDER_AUTHORITY", "\"$providerAuthority\"")
        buildConfigField("String", "CORE_VERSION", "\"touchHLE 0.3.0\"")
        manifestPlaceholders["providerAuthority"] = providerAuthority

        externalNativeBuild {
            ndkBuild {
                arguments("APP_PLATFORM=android-21")
                abiFilters("arm64-v8a")
            }
        }
        ndk {
            abiFilters.add("arm64-v8a")
        }
    }

    signingConfigs {
        create("release") {
            if (releaseKeystorePath != null && file(releaseKeystorePath).exists()) {
                storeFile = file(releaseKeystorePath)
                storePassword = System.getenv("NX_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("NX_KEY_ALIAS")
                keyPassword = System.getenv("NX_KEY_PASSWORD")
            } else {
                val debugSigning = getByName("debug")
                storeFile = debugSigning.storeFile
                storePassword = debugSigning.storePassword
                keyAlias = debugSigning.keyAlias
                keyPassword = debugSigning.keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isDebuggable = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    sourceSets {
        getByName("main") {
            java.srcDir(coreDirectory.resolve("vendor/SDL/android-project/app/src/main/java"))
            assets.srcDir(generatedCoreAssetsDirectory)
        }
    }

    externalNativeBuild {
        ndkBuild {
            path("jni/Android.mk")
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        abortOnError = false
    }

    applicationVariants.all {
        val variantName = name.replaceFirstChar { character -> character.titlecase() }
        tasks.named("merge${variantName}Assets").configure {
            dependsOn("externalNativeBuild$variantName")
            dependsOn("copyCoreAssets")
        }
    }
}

ksp {
    arg("room.schemaLocation", projectDir.resolve("schemas").path)
}

val copyCoreAssets by tasks.registering(Copy::class) {
    from(coreDirectory) {
        include("touchHLE_dylibs/**")
        include("touchHLE_fonts/**")
        include("touchHLE_default_options.txt")
    }
    into(generatedCoreAssetsDirectory)
}

tasks.named("preBuild").configure {
    dependsOn(copyCoreAssets)
}

cargoNdk {
    targets = arrayListOf("arm64")
    module = "../core/touchHLE"
    apiLevel = 21
    buildType = "release"
    librariesNames = arrayListOf("libtouchHLE.so", "libSDL2.so", "libc++_shared.so")
    extraCargoEnv = mapOf(
        "ANDROID_NDK" to android.ndkDirectory.toString(),
        "ANDROID_NDK_HOME" to android.ndkDirectory.toString(),
        "NX_ANDROID_PROVIDER_AUTHORITY" to providerAuthority,
    )
    extraCargoBuildArguments = arrayListOf(
        "--lib",
        "--no-default-features",
        "--features",
        "touchHLE_openal_soft_wrapper/static,sdl2/bundled",
    )
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("com.googlecode.plist:dd-plist:1.28")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
