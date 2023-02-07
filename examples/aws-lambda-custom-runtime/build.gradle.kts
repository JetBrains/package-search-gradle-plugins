plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.graalvm.buildtools.native)
    id("org.jetbrains.gradle.aws-lambda-native-runtime")
}

kotlin.target.compilations.all {
    kotlinOptions {
        jvmTarget = "17"
    }
}

aws {
    lambdas {
        main {
            entryClass.set("org.jetbrains.gradle.awslambdaruntime.MainKt")
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(libs.logback.classic)
    implementation(libs.kotlin.aws.lambda.runtime.client)
}
