@file:Suppress("SpellCheckingInspection", "ConvertToStringTemplate", "PropertyName", "LocalVariableName")

import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.compose.ExperimentalComposeLibrary

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

group = "com.zoffcc.applications.toloshare_material"
version = "1.0.12"
val appName = "toloshare_material"

val build_with_appimage = false

var os: OperatingSystem? = null
var os_arch: String? = null
var os_java_home: String? = null
var os_java_runtime_version: String? = null
var os_java_vm_version: String? = null


repositories {
    flatDir {
        dirs("customlibs")
    }
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
    maven("https://jitpack.io")
}


dependencies {
    implementation(compose.desktop.currentOs)
    @Suppress("DEPRECATION")
    implementation(compose.desktop.common)
    @Suppress("DEPRECATION")
    implementation(compose.ui)
    @Suppress("DEPRECATION")
    implementation(compose.runtime)
    @Suppress("DEPRECATION")
    implementation(compose.foundation)
    @Suppress("DEPRECATION")
    implementation(compose.material)
    @Suppress("DEPRECATION")
    implementation(compose.material3)
    @Suppress("OPT_IN_IS_NOT_ENABLED", "DEPRECATION")
    @OptIn(ExperimentalComposeLibrary::class)
    implementation(compose.components.resources)
    //
    @Suppress("DEPRECATION")
    implementation(compose.materialIconsExtended)
    //
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines:0.19.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
}

val main_class_name = "ToLoShareMainKt"

compose.desktop {
    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    kotlin {
        jvmToolchain(17)
    }

    application {

        mainClass = main_class_name
        // jvmArgs += listOf("-Xmx2G")
        // args += listOf("-customArgument")
        jvmArgs += listOf("-Dcom.apple.mrj.application.apple.menu.about.name=ToLoShare")
        jvmArgs += listOf("-Dapple.awt.application.name=ToLoShare")

        buildTypes.release.proguard {
            optimize.set(false)
            obfuscate.set(false)
            configurationFiles.from("proguard-rules.pro")
        }

        nativeDistributions {
            packageName = appName
            packageVersion = "${project.version}"
            println("packageVersion=$packageVersion")
            description = "ToLoShare Material App"
            copyright = "© 2023 Zoff. All rights reserved."
            vendor = "Zoxcore"
            licenseFile.set(project.file("LICENSE"))
            println("licenseFile=" + project.file("LICENSE"))
            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))

            if (build_with_appimage)
            {
                println("#### build with AppImage ####")
                targetFormats(
                    TargetFormat.Msi, TargetFormat.Exe,
                    TargetFormat.Dmg,
                    TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.AppImage
                )
            }
            else
            {
                println("==== build without AppImage ====")
                targetFormats(
                    TargetFormat.Msi, TargetFormat.Exe,
                    TargetFormat.Dmg,
                    TargetFormat.Deb, TargetFormat.Rpm
                )
            }

            nativeDistributions {
                modules("java.instrument", "java.net.http", "java.prefs", "java.sql", "jdk.unsupported", "jdk.security.auth")
                // includeAllModules = true
            }

            val iconsRoot = project.file("resources")
            linux {
                iconFile.set(iconsRoot.resolve("icon-linux.png"))
                println("iconFile=" + iconsRoot.resolve("icon-linux.png"))
            }
            println("targetFormats=" + targetFormats)

            // XX // jvmArgs += "-splash:resources/splash_screen.png"
            // XX // jvmArgs += "-splash:${'$'}APPDIR/app/resources/splash_screen.png"
            // XX // jvmArgs += "-splash:" + iconsRoot.resolve("splash_screen.png")
            // -----------------------------------------------------------------
            // --> for .deb -->
            jvmArgs += "-splash:${'$'}APPDIR/resources/splash_screen.png"
            // --> for gradlew run --> // jvmArgs += "-splash:resources/splash_screen.png"
            // -----------------------------------------------------------------
            println("jvmArgs=" + jvmArgs)
            // val ENV = System.getenv()
            // println("ENV_all=" + ENV.keys)
        }
    }
}

tasks.withType<org.gradle.jvm.tasks.Jar> {
    manifest {
        attributes["SplashScreen-Image"] = "splash_screen.png"
    }
}

