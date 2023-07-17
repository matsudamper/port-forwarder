import groovy.lang.Closure
import io.github.fvarrui.javapackager.model.HeaderType
import io.github.fvarrui.javapackager.model.LinuxConfig
import io.github.fvarrui.javapackager.model.MacConfig
import io.github.fvarrui.javapackager.model.Platform
import io.github.fvarrui.javapackager.model.WindowsConfig
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.plugin.allopen") version "1.8.20"
    kotlin("jvm") version "1.8.20"
    id("org.graalvm.buildtools.native") version "0.9.19"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.20"
    application
    alias(libs.plugins.ktlint)
}

buildscript {
    dependencies {
        classpath("com.jakewharton.mosaic:mosaic-gradle-plugin:0.6.0")
        classpath("io.github.fvarrui:javapackager:1.7.2")
    }
}

apply(plugin = "com.jakewharton.mosaic")
apply(plugin = "io.github.fvarrui.javapackager.plugin")

base.archivesName.set("portforward")
group = "net.matsudamper.portforward"
version = "1.0-SNAPSHOT"

application {
    applicationDefaultJvmArgs = listOf(
        "-agentlib:native-image-agent=config-merge-dir=src/main/resources/META-INF/native-image/auto/",
    )
    mainClass.set("net.matsudamper.portforward.MainKt")
}

tasks.withType(Jar::class) {
    manifest {
        attributes["Main-Class"] = "net.matsudamper.portforward.MainKt"
    }
    from(
        configurations.runtimeClasspath.map {
            it.toList().map {
                if (it.isDirectory) it else zipTree(it)
            }
        },
    ) {
        exclude(
            "META-INF/*.SF",
            "META-INF/*.DSA",
            "META-INF/*.RSA",
        )
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2")
    implementation("org.slf4j:slf4j-api:2.0.7")


    implementation(kotlin("stdlib"))

    implementation("com.charleskorn.kaml:kaml:0.53.0")
    implementation("org.apache.sshd:sshd-mina:2.10.0")
    implementation("org.jline:jline:3.23.0")

    val ktorVersion = "2.2.4"
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-cio:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-default-headers:$ktorVersion")
    implementation("io.ktor:ktor-server-forwarded-header:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs = listOf(
        "-agentlib:native-image-agent=config-merge-dir=src/main/resources/META-INF/native-image/auto/",
    )
}

allprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {

    }
}
tasks.withType<Jar> {
    archiveBaseName.set(base.archivesName)
    archiveVersion.set("")
}

val kotlinCompile = tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()
}

tasks.create<io.github.fvarrui.javapackager.gradle.PackageTask>("javapackage") {
    dependsOn(kotlinCompile)
    doFirst {
        outputDirectory.deleteRecursively()
    }
    mainClass = application.mainClass.get()
    isCustomizedJre = true
    isBundleJre = true
    isCopyDependencies = false
    isGenerateInstaller = true
    isAdministratorRequired = false
    platform = Platform.auto
    additionalResources = listOf()
    linuxConfig = LinuxConfig().also {
        it.isWrapJar = true
        it.isGenerateDeb = false
        it.isGenerateRpm = false
    }
    macConfig = MacConfig()
    outputDirectory = File(buildDir, "javapackage")

    @Suppress("UNCHECKED_CAST")
    winConfig(
        closureOf<WindowsConfig> {
            headerType = HeaderType.console
            isWrapJar = true
            isGenerateMsi = true
        } as Closure<WindowsConfig>,
    )
}

graalvmNative {
    binaries {
        named("main") {
            javaLauncher.set(
                javaToolchains.launcherFor {
                    languageVersion.set(JavaLanguageVersion.of(17))
                    vendor.set(JvmVendorSpec.matching("GraalVM Community"))
                },
            )
            imageName.set(base.archivesName.get())
            mainClass.set(application.mainClass.get())

            buildArgs.addAll(
                "--initialize-at-build-time=org.slf4j.LoggerFactory",
            )
        }
    }
}
