plugins {
    id("com.android.application") version "8.10.1" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
    id("com.github.willir.rust.cargo-ndk-android") version "0.3.4" apply false
}

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}
