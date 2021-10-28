plugins {
    id("org.jetbrains.gradle.liquibase")
}

val liquibaseVersion: String by extra
val sqliteDriversVersion: String by extra

dependencies {
    liquibaseRuntime("org.liquibase:liquibase-core:$liquibaseVersion")
    liquibaseRuntime("org.xerial:sqlite-jdbc:$sqliteDriversVersion")
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
