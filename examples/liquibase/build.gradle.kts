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
            properties {
                changeLogFile.set("dbchangelog.postgresql.xml")
                driver.set("org.sqlite.JDBC")
            }
        }

        register("dio") {
            properties {
                url.set("jdbc:sqlite:./db.sql")
                password.set("banana")
                username.set("mamma mia")
            }
        }
    }
}
