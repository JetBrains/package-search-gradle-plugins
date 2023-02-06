import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

buildscript {
    dependencies {
        classpath(libs.kotlinpoet)
    }
}

plugins {
    `kotlin-dsl`
    `maven-publish`
    alias(libs.plugins.gradle.publish)
}

group = "org.jetbrains.gradle"
version = System.getenv("GITHUB_REF")?.substringAfterLast("/") ?: "0.0.1"

dependencies {
    api(kotlin("reflect"))
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
        }
    }
}

java {
    targetCompatibility = JavaVersion.VERSION_11
    sourceCompatibility = JavaVersion.VERSION_11
}

pluginBundle {
    website = "https://github.com/JetBrains/jetbrains-gradle-plugins"
    vcsUrl = "https://github.com/JetBrains/jetbrains-gradle-plugins.git"
    tags = listOf("docker", "container", "terraform", "cloud", "aws", "azure", "google", "liquibase", "migrations")
}

gradlePlugin {
    plugins {
        create("dockerPlugin") {
            id = "org.jetbrains.gradle.docker"
            displayName = "JetBrains Docker Plugin"
            description = "Build and push Docker images from your build."
            implementationClass = "org.jetbrains.gradle.plugins.docker.DockerPlugin"
        }
        create("terraformPlugin") {
            id = "org.jetbrains.gradle.terraform"
            displayName = "JetBrains Terraform Plugin"
            description = "Source sets plugin for controlling terraform projects from Gradle, batteries included."
            implementationClass = "org.jetbrains.gradle.plugins.terraform.TerraformPlugin"
        }
        create("liquibasePlugin") {
            id = "org.jetbrains.gradle.liquibase"
            displayName = "JetBrains Liquibase Plugin"
            description = "Run migrations from Gradle using the Liquibase runtime."
            implementationClass = "org.jetbrains.gradle.plugins.liquibase.LiquibasePlugin"
        }
        create("upxPlugin") {
            id = "org.jetbrains.gradle.upx"
            displayName = "JetBrains UPX Plugin"
            description = "Compress your native executables usin UPX"
            implementationClass = "org.jetbrains.gradle.plugins.upx.UpxPlugin"
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