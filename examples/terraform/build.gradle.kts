plugins {
    id("org.jetbrains.gradle.terraform")
}

terraform {
    sourceSets {
        main {
            commands {
                terraformInit {
                    useBackend = false
                }
            }
        }
    }
}