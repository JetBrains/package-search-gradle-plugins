import kotlin.random.Random

plugins {
    id("org.jetbrains.gradle.terraform")
}

terraform {
    executeApplyOnlyIf { System.getenv("ENABLE_TF_APPLY") == "true" }
    sourceSets {
        main {
            planVariables = mapOf("lambdaDirectory" to lambdasDirectory.absolutePath)
            srcDir = file("src/main/terraform")
            commands {
                terraformInit {
                    onlyIf { Random.nextBoolean() }
                }
            }
        }
    }
}
