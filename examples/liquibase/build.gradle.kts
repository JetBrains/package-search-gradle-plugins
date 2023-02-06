plugins {
    id("org.jetbrains.gradle.liquibase")
}

dependencies {
    liquibaseRuntime(libs.liquibase.core)
    liquibaseRuntime(libs.sqlite.jdbc)
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
