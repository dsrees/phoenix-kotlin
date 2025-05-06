import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinKover)
    alias(libs.plugins.mavenPublish)
}

group = "com.github.dsrees.phoenix"
version = "0.1.0"

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_1_8)
                }
            }
        }
    }

    val xcf = XCFramework()
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = "phoenix-core"
            xcf.add(this)
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // put your multiplatform dependencies here
            implementation(libs.kotlin.serialization.json)
            implementation(libs.ktor.client.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.github.dsrees.phoenix"
    compileSdk = 35
    defaultConfig {
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

mavenPublishing {
    coordinates("com.github.dsrees", "phoenix-kotlin", version.toString())

    pom {
        name = "phoenix-kkotlin"
        description = "A Phoenix Channels client for Kotlin Multiplatform."
        url = "https://github.com/dsrees/phoenix-kotlin"
        inceptionYear = "2025"

        licenses {
            license {
                name = "MIT License"
                url = "https://opensource.org/license/MIT"
                distribution = "https://opensource.org/license/MIT"
            }
        }

        scm {
            connection = "scm:git:git://github.com/dsrees/phoenix-kotlin.git"
            developerConnection = "scm:git:ssh://github.com/dsrees/phoenix-kotlin.git"
            url = "https://github.com/dsrees/phoenix-kotlin"
        }

        developers {
            developer {
                name = "Daniel Rees"
                email = "daniel.rees18@gmail.com"
                url = "https://github.com/dsrees"
            }
        }
    }
}
