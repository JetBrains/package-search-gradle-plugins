@file:Suppress("UnstableApiUsage")

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    dependencies {
        classpath(libs.kotlinpoet)
    }
}

plugins {
    `kotlin-dsl`
    `maven-publish`
    alias(libs.plugins.gradle.publish)
    alias(libs.plugins.kotlin.plugin.serialization)
}

group = "org.jetbrains.gradle"
version = System.getenv("GITHUB_REF")?.substringAfterLast("/") ?: "0.0.1"

dependencies {
    api(kotlin("reflect"))
    api(kotlin("gradle-plugin"))
    api(libs.docker.java)
    api(libs.docker.java.transport.httpclient5)
    api(libs.kotlinx.serialization.json)
    api(libs.tukaani.xz)
    implementation(libs.graalvm.native.gradle.plugin)
}

val generatedDir = buildDir.resolve("generated/main/kotlin")

kotlin {
    target {
        compilations.all {
            kotlinOptions {
                languageVersion = "1.5"
                jvmTarget = "11"
            }
        }
    }
    sourceSets {
        main {
            kotlin.srcDir(generatedDir)
        }
        all {
            languageSettings.optIn("kotlin.ExperimentalStdlibApi")
            languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
        }
    }
}

java {
    targetCompatibility = JavaVersion.VERSION_11
    sourceCompatibility = JavaVersion.VERSION_11
}

gradlePlugin {
    website.set("https://github.com/JetBrains/jetbrains-gradle-plugins")
    vcsUrl.set("https://github.com/JetBrains/jetbrains-gradle-plugins.git")
    plugins {
        create("dockerPlugin") {
            id = "org.jetbrains.gradle.docker"
            displayName = "JetBrains Docker Plugin"
            description = "Build and push Docker images from your build."
            implementationClass = "org.jetbrains.gradle.plugins.docker.DockerPlugin"
            tags.set(listOf("docker", "container"))
        }
        create("terraformPlugin") {
            id = "org.jetbrains.gradle.terraform"
            displayName = "JetBrains Terraform Plugin"
            description = "Source sets plugin for controlling terraform projects from Gradle, batteries included."
            implementationClass = "org.jetbrains.gradle.plugins.terraform.TerraformPlugin"
            tags.set(listOf("terraform", "cloud", "aws", "azure", "google"))
        }
        create("liquibasePlugin") {
            id = "org.jetbrains.gradle.liquibase"
            displayName = "JetBrains Liquibase Plugin"
            description = "Run migrations from Gradle using the Liquibase runtime."
            implementationClass = "org.jetbrains.gradle.plugins.liquibase.LiquibasePlugin"
            tags.set(listOf("liquibase", "migrations"))
        }
        create("upxPlugin") {
            id = "org.jetbrains.gradle.upx"
            displayName = "JetBrains UPX Plugin"
            description = "Compress your native executables using UPX"
            implementationClass = "org.jetbrains.gradle.plugins.upx.UpxPlugin"
            tags.set(listOf("upx", "compression"))
        }
        create("nativeRuntime") {
            id = "org.jetbrains.gradle.aws-lambda-native-runtime"
            displayName = "JetBrains Aws Lambda Custom Native Runtime Plugin"
            description = "Create a custom AWS Lambda runtime using GraalVM native-image and UPX"
            implementationClass = "org.jetbrains.gradle.plugins.nativeruntime.AwsLambdaNativeRuntimePlugin"
            tags.set(listOf("aws", "aws-lambda"))
        }
    }
}

val generatePluginIds by tasks.registering {
    outputs.dir(generatedDir)
    doFirst {
        val fileBuilder = FileSpec.builder("plugins.gradle.jetbrains.org", "PluginIds")
        gradlePlugin.plugins.forEach {
            fileBuilder.addProperty(
            PropertySpec.builder("${it.name}Id", String::class)
                .getter(
                    FunSpec.getterBuilder()
                        .addCode("return %S", it.id)
                        .build()
                )
                .build()
            )
        }
        fileBuilder.build().writeTo(generatedDir)
    }
}



tasks.withType<KotlinCompile> {
    dependsOn(generatePluginIds)
}

publishing {
    repositories {
        mavenLocal()
    }
    publications {
        afterEvaluate {
            named<MavenPublication>("pluginMaven") {
                artifactId = rootProject.name
            }
        }
    }
}
