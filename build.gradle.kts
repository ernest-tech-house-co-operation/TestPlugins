import com.android.build.api.dsl.LibraryExtension
import com.lagradost.cloudstream3.gradle.CloudstreamExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }

    dependencies {
        classpath("com.android.tools.build:gradle:9.1.1")
        // Pinned commit instead of -SNAPSHOT for reproducible builds
        classpath("com.github.recloudstream.cloudstream:gradle:v4.7.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.0")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

fun Project.cloudstream(configuration: CloudstreamExtension.() -> Unit) =
    extensions.getByName<CloudstreamExtension>("cloudstream").configuration()

fun Project.android(configuration: LibraryExtension.() -> Unit) {
    extensions.getByName<LibraryExtension>("android").apply {
        project.extensions.findByType(JavaPluginExtension::class.java)?.apply {
            // Use Java 17 toolchain even if a higher JDK runs the build.
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(17))
            }
        }
        configuration()
    }
}

subprojects {
    apply(plugin = "com.android.library")
    apply(plugin = "com.lagradost.cloudstream3.gradle")

    cloudstream {
        setRepo(System.getenv("GITHUB_REPOSITORY") ?: "ernest-tech-house-co-operation/TestPlugins")
        authors = listOf("Pease Ernest", "ErnestTech House")
    }

    android {
        namespace = "com.ernest"
        compileSdk = 36

        defaultConfig {
            minSdk = 21
            targetSdk = 35
        }

        lint {
            targetSdk = 36
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }

        //noinspection WrongGradleMethod
        tasks.withType<KotlinJvmCompile> {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_1_8)
                freeCompilerArgs.addAll(
                    "-Xno-call-assertions",
                    "-Xno-param-assertions",
                    "-Xno-receiver-assertions",
                )
            }
        }
    }

    dependencies {
        val implementation by configurations
        val cloudstream by configurations

        cloudstream("com.lagradost:cloudstream3:pre-release")

        implementation(kotlin("stdlib"))
        implementation("com.github.Blatzar:NiceHttp:0.4.18")
        implementation("org.jsoup:jsoup:1.22.2")
        implementation("androidx.annotation:annotation:1.10.0")

        // Do not bump above 2.13.1 (breaks compat on older Android devices)
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1")
        implementation("com.fasterxml.jackson.core:jackson-databind:2.13.1")

        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
        implementation("com.google.code.gson:gson:2.14.0")

        // Your project's existing deps
        implementation("com.google.android.material:material:1.12.0")
        implementation("androidx.recyclerview:recyclerview:1.3.2")
        implementation("io.ktor:ktor-client-core:2.3.7")
        implementation("io.ktor:ktor-client-cio:2.3.7")
        implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
        implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    }
}

task<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}