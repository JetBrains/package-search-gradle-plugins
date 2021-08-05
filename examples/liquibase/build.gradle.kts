import org.jetbrains.gradle.plugins.liquibase.LiquibaseCommand

plugins {
    id("org.jetbrains.gradle.liquibase")
}

dependencies {
    liquibaseRuntime("org.liquibase:liquibase-core:4.4.2")
    liquibaseRuntime("org.postgresql:postgresql:42.2.23}")
}

liquibase {
    activities {

        all {
            arguments["changeLogFile"] = "dbchangelog.postgresql.xml"
            arguments["driver"] = "org.postgresql.Driver"
        }

        register("local") {
            arguments["url"] = "jdbc:postgresql://localhost:5432/indexer"
            arguments["username"] = "postgres"
            arguments["password"] = "whatever"
        }
    }
}
