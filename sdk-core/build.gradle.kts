import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

val sdkVersion =
    providers.gradleProperty("SDK_VERSION").get()

group = "com.github.marcianojm-ai"
version = sdkVersion

android {
    namespace = "com.apppayback.sdk"

    compileSdk = 35

    defaultConfig {
        minSdk = 23

        buildConfigField(
            "String",
            "SDK_VERSION",
            "\"$sdkVersion\""
        )

        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        getByName("debug") {
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"http://127.0.0.1:5001/android-tester-saas-prod/us-central1/api/sdk\""
            )
        }

        getByName("release") {
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"https://apppaybacktesting.web.app/sdk\""
            )
        }
    }

    // Bytecode Java 8 e Kotlin 1.8: compatível com apps em versões antigas
    // do Kotlin e do Android Gradle Plugin.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
        languageVersion.set(KotlinVersion.KOTLIN_1_8)
        apiVersion.set(KotlinVersion.KOTLIN_1_8)
    }
}

dependencies {
    // Versão mínima da biblioteca Kotlin; o app pode usar uma mais nova.
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.22")
    testImplementation("junit:junit:4.13.2")
    implementation("com.android.installreferrer:installreferrer:2.2")
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                from(components["release"])
                groupId = project.group.toString()
                artifactId = "testing-sdk"
                version = sdkVersion

                pom {
                    name.set("AppPayBack Android SDK")
                    description.set(
                        "Android SDK for AppPayBack participant linking and daily check-ins."
                    )
                    url.set("https://github.com/marcianojm-ai/testing-sdk")
                }
            }
        }
    }
}
