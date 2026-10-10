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
    namespace = "com.suaempresa.testing"

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
                "\"http://127.0.0.1:5001/android-tester-saas-prod/us-central1/api\""
            )
        }

        getByName("release") {
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"https://us-central1-android-tester-saas-prod.cloudfunctions.net/api\""
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
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
                        "SDK Android para vínculo de participantes e check-ins do AppPayBack."
                    )
                    url.set("https://github.com/marcianojm-ai/testing-sdk")
                }
            }
        }
    }
}
