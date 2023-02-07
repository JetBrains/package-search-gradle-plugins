plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.graalvm.buildtools.native)
    application
    id("org.jetbrains.gradle.native-runtime")
}

kotlin.target.compilations.all {
    kotlinOptions {
        jvmTarget = "17"
    }
}

aws {
    lambdas {
        create("helloWorld") {
            entryClass.set("org.jetbrains.gradle.awslambdaruntime.ExampleHandler")
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(libs.aws.lambda.java.core)
    implementation(libs.aws.lambda.java.events)
    implementation(libs.aws.lambda.java.runtime.`interface`.client)
    implementation(libs.kotlinx.serialization.json)
}

println(System.getProperty("os.arch"))