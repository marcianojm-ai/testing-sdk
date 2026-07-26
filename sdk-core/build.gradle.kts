plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish") // Plugin para o JitPack
}

android {
    namespace = "com.suaempresa.testing"

    compileSdk = 35

    defaultConfig {
        minSdk = 23
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Inicialização autônoma do SDK
    implementation("androidx.startup:startup-runtime:1.2.0")

    // Biblioteca oficial do Google para ler o Referrer de instalação
    implementation("com.android.installreferrer:installreferrer:2.2")
}

// Configuração de publicação para o JitPack ler a biblioteca
afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                afterEvaluate {
                    from(components["release"])
                }
            }
        }
    }
}