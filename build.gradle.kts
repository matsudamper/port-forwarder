plugins {
    id("org.jetbrains.kotlin.plugin.allopen") version "1.8.20"
    kotlin("jvm") version "1.8.20"
    id("org.graalvm.buildtools.native") version "0.9.18"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.20"
    application
    alias(libs.plugins.ktlint)
}

buildscript {
    dependencies {
        classpath("com.jakewharton.mosaic:mosaic-gradle-plugin:0.6.0")
    }
}
apply(plugin = "com.jakewharton.mosaic")

base.archivesName.set("portfoward")
group = "net.matsudamper.portfoward"
version = "1.0-SNAPSHOT"

application {
    applicationDefaultJvmArgs = listOf(
        "-agentlib:native-image-agent=config-merge-dir=src/main/resources/META-INF/native-image/auto/",
    )
    mainClass.set("net.matsudamper.portfoward.MainKt")
}

tasks.withType(Jar::class) {
    manifest {
        attributes["Main-Class"] = "net.matsudamper.portfoward.MainKt"
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
}

allprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {

    }
}

graalvmNative {
    binaries {
        named("main") {
            javaLauncher.set(
                javaToolchains.launcherFor {
                    languageVersion.set(JavaLanguageVersion.of(17))
                    vendor.set(JvmVendorSpec.GRAAL_VM)
                },
            )
            imageName.set(base.archivesName.get())
            mainClass.set("MainKt")

            buildArgs.addAll(
                "-H:ReflectionConfigurationFiles=${projectDir}/reflection-config.json",
                "-H:ResourceConfigurationFiles=${projectDir}/resource-config.json",
            )
        }
    }
}
