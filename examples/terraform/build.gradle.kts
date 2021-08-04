plugins {
    id("org.jetbrains.gradle.terraform")
}

terraform {
    sourceSets {
        main {
            planVariables = mapOf("lambdaDirectory" to lambdasDirectory.absolutePath)
            commands {
                terraformInit {

                }
            }
        }
    }
}
