plugins {
    id("org.jetbrains.gradle.liquibase")
}

dependencies {
    liquibaseRuntime("org.liquibase:liquibase-core:4.4.2")
    liquibaseRuntime("org.xerial:sqlite-jdbc:3.36.0.1")
}

liquibase {
    activities {

        all {
            arguments["changeLogFile"] = "dbchangelog.postgresql.xml"
            arguments["driver"] = "org.sqlite.JDBC"
        }

        register("local") {
            arguments["url"] = "jdbc:sqlite:./db.sql"
        }
    }
}
