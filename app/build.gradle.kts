plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Kode game (library native) dari cry-of-fear-src. Workflow meng-clone-nya ke cof-src/.
// Kalau folder itu tidak ada, build tetap jalan sebagai launcher saja.
val cofCmake = rootProject.file("cof-src/src/cof/CMakeLists.txt")

android {
    namespace = "com.cof.launcher"
    compileSdk = 34
    if (cofCmake.exists()) ndkVersion = "29.0.14206865"

    defaultConfig {
        applicationId = "com.cof.launcher"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "3.0"

        if (cofCmake.exists()) {
            ndk { abiFilters += listOf("arm64-v8a") }
            externalNativeBuild {
                cmake {
                    arguments(
                        "-DGAMEDIR=cryoffear",
                        "-DUSE_VGUI=OFF",
                        "-DUSE_NOVGUI_MOTD=ON",
                        "-DUSE_NOVGUI_SCOREBOARD=ON"
                    )
                }
            }
        }
    }

    if (cofCmake.exists()) {
        externalNativeBuild {
            cmake {
                path = cofCmake
                version = "3.22.1"
            }
        }
    }

    // Library harus diekstrak ke folder lib supaya engine bisa memuat library game.
    packaging { jniLibs { useLegacyPackaging = true } }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:1.9.0")
}
