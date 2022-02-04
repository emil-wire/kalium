plugins {
    Plugins.androidLibrary(this)
    Plugins.multiplatform(this)
    Plugins.serialization(this)
    Plugins.sqlDelight(this)
}

group = "com.wire.kalium"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-native-utils:1.6.0")
}

sqldelight {
    database("AppDatabase") {
        dialect = "sqlite:3.24"
        packageName = "com.wire.kalium.persistence.db"
    }
}

android {
    compileSdk = Android.Sdk.compile
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = Android.Sdk.min
        targetSdk = Android.Sdk.target
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    // Remove Android Unit tests, as it's currently impossible to run native-through-NDK code on simple Unit tests.
    sourceSets.remove(sourceSets["test"])
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
            kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
        }
        testRuns["test"].executionTask.configure {
            useJUnit()

            testLogging {
                showStandardStreams = true
            }
        }
    }
    android()
    iosX64()
    js(IR) {
        browser {
            testTask {
                useMocha {
                    timeout = "5s"
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // coroutines
                implementation(Dependencies.Coroutines.core) {
                    version {
                        // strictly using the native-mt version on coroutines
                        strictly(Versions.coroutines)
                    }
                }
                implementation(Dependencies.SqlDelight.runtime)
                implementation(Dependencies.SqlDelight.coroutinesExtension)
                implementation(Dependencies.Kotlinx.serialization)
                implementation(Dependencies.MultiplatformSettings.settings)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                // coroutines
                implementation(Dependencies.Coroutines.test)
                // MultiplatformSettings
                implementation(Dependencies.MultiplatformSettings.test)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(Dependencies.SqlDelight.jvmDriver)
            }
        }
        val jvmTest by getting
        val jsMain by getting {
            dependencies {
                implementation(Dependencies.SqlDelight.jsDriver)
                implementation(npm("sql.js", "1.6.2"))
                implementation(devNpm("copy-webpack-plugin", "9.1.0"))
            }
        }
        val jsTest by getting
        val androidMain by getting {
            dependencies {
                implementation(Dependencies.Android.securityCrypto)
                implementation(Dependencies.SqlDelight.androidDriver)
                implementation("net.zetetic:android-database-sqlcipher:4.5.0@aar")
                implementation("androidx.sqlite:sqlite:2.0.1")
            }
        }
        val androidAndroidTest by getting {
            dependencies {
                implementation(Dependencies.AndroidInstruments.androidTestRunner)
                implementation(Dependencies.AndroidInstruments.androidTestRules)
                implementation(Dependencies.AndroidInstruments.androidTestCore)
            }
        }

        val iosX64Main by getting {
            dependencies {
                implementation(Dependencies.SqlDelight.nativeDriver)
            }
        }
        val iosX64Test by getting
    }
}