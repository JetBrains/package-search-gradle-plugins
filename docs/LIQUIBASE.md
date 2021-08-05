## Liquibase plugin

Mange your migrations with Gradle:

```kotlin
plugins {
    id("com.jetbrains.packagesearch.liquibase")
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

        register("production") {
            arguments["url"] = System.getenv("PRODUCTION_DB_URL")
            arguments["username"] = System.getenv("PRODUCTION_DB_USERNAME")
            arguments["password"] = System.getenv("PRODUCTION_DB_PASSWORD")
        }

        register("staging") {
            arguments["url"] = System.getenv("STAGING_DB_URL")
            arguments["username"] = System.getenv("STAGING_DB_USERNAME")
            arguments["password"] = System.getenv("STAGING_DB_PASSWORD")
        }

        register("local") {
            arguments["url"] = "jdbc:postgresql://localhost:5432/indexer"
            arguments["username"] = "postgres"
            arguments["password"] = "whatever"
        }
    }
}
```

For each activity a task of each existing Liquibase command will be created.
