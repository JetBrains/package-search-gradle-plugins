import org.jetbrains.gradle.plugins.liquibase.LiquibaseCommand

plugins {
    id("org.jetbrains.gradle.liquibase")
}

liquibase {
    activities {
        create("banana") {
            onCommand(LiquibaseCommand.UPDATE) {
                doFirst {

                }
                doLast {

                }
            }
        }
    }
}
