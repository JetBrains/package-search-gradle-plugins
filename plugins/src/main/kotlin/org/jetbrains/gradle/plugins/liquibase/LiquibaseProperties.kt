package org.jetbrains.gradle.plugins.liquibase

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import org.jetbrains.gradle.plugins.property
import org.jetbrains.gradle.plugins.writeText
import java.io.File

open class LiquibaseProperties : DefaultTask() {

    @get:OutputFile
    val outputPropertiesFile = project.objects.property<File>()

    /**
     * Specifies the path to the changelog to execute.
     **/
    @get:Input
    val changeLogFile: Property<String> = project.objects.property()

    /**
     * Specifies the driver class name for your target database.
     **/
    @get:Input
    val driver: Property<String> = project.objects.property()

    /**
     * Specifies the source database for performing comparisons.
     **/
    @get:Input
    @get:Optional
    val referenceUrl: Property<String> = project.objects.property()

    /**
     * Specifies the username for your target database.
     **/
    @get:Input
    val username: Property<String> = project.objects.property()

    /**
     * Specifies the password for your target database.
     **/
    @get:Input
    val password: Property<String> = project.objects.property()

    /**
     * Specifies the driver class name for your source database.
     **/
    @get:Input
    @get:Optional
    val referenceDriver: Property<String> = project.objects.property()

    /**
     * Specifies the database you want to use to compare to your source database. Also known as your target.
     **/
    @get:Input
    val url: Property<String> = project.objects.property()

    /**
     * Specifies the username for your source database.
     **/
    @get:Input
    @get:Optional
    val referenceUsername: Property<String> = project.objects.property()

    /**
     * Specifies the password for your source database.
     **/
    @get:Input
    @get:Optional
    val referencePassword: Property<String> = project.objects.property()

    /**
     * Specifies your Liquibase Pro license key (If you have one).
     **/
    @get:Input
    @get:Optional
    val liquibaseProLicenseKey: Property<String> = project.objects.property()

    /**
     * Specifies the directories and jar files to search for changelog files and custom extension classes. Multiple
     * directories can be separated with ; on Windows or : on Linux or MacOS.
     **/
    @get:Input
    @get:Optional
    val classpath: Property<String> = project.objects.property()

    /**
     * Specifies the custom Change Exec Listener implementation to use.
     **/
    @get:Input
    @get:Optional
    val changeExecListenerClass: Property<String> = project.objects.property()

    /**
     * Specifies properties for Change Exec Listener.
     **/
    @get:Input
    @get:Optional
    val changeExecListenerPropertiesFile: Property<String> = project.objects.property()

    /**
     * Specifies the author of auto-generated changesets.
     **/
    @get:Input
    @get:Optional
    val changeSetAuthor: Property<String> = project.objects.property()

    /**
     * "Specifies the execution context to be used for changesets in the generated changelog, which can be "",""
     * separated if there are multiple contexts."
     **/
    @get:Input
    @get:Optional
    val changeSetContext: Property<String> = project.objects.property()

    /**
     * Specifies changeset Contexts to execute.
     **/
    @get:Input
    @get:Optional
    val contexts: Property<String> = project.objects.property()

    /**
     * Overrides the current date time function used in SQL. Useful for unsupported databases.
     **/
    @get:Input
    @get:Optional
    val currentDateTimeFunction: Property<String> = project.objects.property()

    /**
     * Specifies the Liquibase changelog lock table. Default: DATABASECHANGELOGLOCK.
     **/
    @get:Input
    @get:Optional
    val databaseChangeLogLockTableName: Property<String> = project.objects.property()

    /**
     * Specifies the Liquibase changelog table. Default: DATABASECHANGELOG.
     **/
    @get:Input
    @get:Optional
    val databaseChangeLogTableName: Property<String> = project.objects.property()

    /**
     * Specifies the tablespace where the DATABASECHANGELOGLOCK and DATABASECHANGELOG tables will be created if they
     * don't exist yet. You can use the property with the Oracle database.
     **/
    @get:Input
    @get:Optional
    val databaseChangeLogTablespaceName: Property<String> = project.objects.property()

    /**
     * Specifies a custom database implementation to use.
     **/
    @get:Input
    @get:Optional
    val databaseClass: Property<String> = project.objects.property()

    /**
     * Specifies the directory where insert statement csv files will be kept. Required by the generateChangeLog command.
     **/
    @get:Input
    @get:Optional
    val dataOutputDirectory: Property<String> = project.objects.property()

    /**
     * Specifies the default catalog name to use for the database connection.
     **/
    @get:Input
    @get:Optional
    val defaultCatalogName: Property<String> = project.objects.property()

    /**
     * Specifies the default schema name to use for the database connection.
     **/
    @get:Input
    @get:Optional
    val defaultSchemaName: Property<String> = project.objects.property()

    /**
     * Sets the string used to break up files that consist of multiple statements. Used with ExecuteSqlCommand.
     **/
    @get:Input
    @get:Optional
    val delimiter: Property<String> = project.objects.property()

    /**
     * Specifies the list of diff types to include in the changelog expressed as a comma-separated list from: tables,
     * views, columns, indexes, foreignkeys, primarykeys, uniqueconstraints, data. If this is null, then the default types will be: tables, views, columns, indexes, foreignkeys, primarykeys, uniqueconstraints. Note:The exact list of options depends on the version and plugins that you have installed. Liquibase Pro provides additional options as well.
     **/
    @get:Input
    @get:Optional
    val diffTypes: Property<String> = project.objects.property()

    /**
     * Specifies the location of a JDBC connection properties file which contains properties that the driver will use.
     **/
    @get:Input
    @get:Optional
    val driverPropertiesFile: Property<String> = project.objects.property()

    /**
     * "Specifies objects to be excluded from the changelog. The example of filters: ""table_name"", ""table:main_.*"",
     * ""column:*._lock, table:primary.*""."
     **/
    @get:Input
    @get:Optional
    val excludeObjects: Property<String> = project.objects.property()

    /**
     * Includes the catalog in the generated changesets. if the value is set to true. Default value is: false.
     **/
    @get:Input
    @get:Optional
    val includeCatalog: Property<Boolean> = project.objects.property()

    /**
     * "Specifies objects to be included in the changelog. The example of filters: ""table_name"", ""table:main_.*"",
     * ""column:*._lock, table:primary.*""."
     **/
    @get:Input
    @get:Optional
    val includeObjects: Property<String> = project.objects.property()

    /**
     * Includes the schema in the generated changesets if the value is set to true. Default value is: false.
     **/
    @get:Input
    @get:Optional
    val includeSchema: Property<Boolean> = project.objects.property()

    /**
     * Include the system classpath in the Liquibase classpath. Default value is: true.
     **/
    @get:Input
    @get:Optional
    val includeSystemClasspath: Property<Boolean> = project.objects.property()

    /**
     * Includes the tablespace of tables and indexes, if the value is set to true. Default value is: false
     **/
    @get:Input
    @get:Optional
    val includeTablespace: Property<Boolean> = project.objects.property()

    /**
     * Filters the changelog using Labels.
     **/
    @get:Input
    @get:Optional
    val labels: Property<String> = project.objects.property()

    /**
     * Specifies the catalog name where the Liquibase tables will be located. The concept of a catalog varies
     * between databases because not all databases have catalogs. For more information, refer to your database
     * documentation.
     **/
    @get:Input
    @get:Optional
    val liquibaseCatalogName: Property<String> = project.objects.property()

    /**
     * Specifies API key for authorization to Hub.
     **/
    @get:Input
    @get:Optional
    val liquibaseHubApiKey: Property<String> = project.objects.property()

    /**
     * Identifies the specific target in which to record your data at Liquibase Hub. The property is available in
     * your Project at https://hub.liquibase.com. Starting from Liquibase 4.4, you can use the following format:
     * liquibase.command.hubConnectionId.
     **/
    @get:Input
    @get:Optional
    val liquibaseHubConnectionId: Property<String> = project.objects.property()

    /**
     * Identifies the specific Project in which to record your data at Liquibase Hub. The property is available in your
     * account at https://hub.liquibase.com. Starting from Liquibase 4.4, you can use the following format:
     * liquibase.command.hubProjectId.
     **/
    @get:Input
    @get:Optional
    val liquibaseHubProjectId: Property<String> = project.objects.property()

    /**
     * Specifies URL to Hub.
     **/
    @get:Input
    @get:Optional
    val liquibaseHubUrl: Property<String> = project.objects.property()

    /**
     * Specifies the schema name where the Liquibase tables will be located.
     **/
    @get:Input
    @get:Optional
    val liquibaseSchemaName: Property<String> = project.objects.property()

    /**
     * Sends logging messages to a file.
     **/
    @get:Input
    @get:Optional
    val logFile: Property<String> = project.objects.property()

    /**
     * Specifies the execution log level (debug, info, warning, severe, off).
     **/
    @get:Input
    @get:Optional
    val logLevel: Property<String> = project.objects.property("info")

    /**
     * Specifies the catalog name that the SQL object references will include, if the value is set to true, even if
     * it is the default catalog.
     **/
    @get:Input
    @get:Optional
    val outputDefaultCatalog: Property<Boolean> = project.objects.property()

    /**
     * Specifies the schema name that the SQL object references will include, if the value is set to true, even if it
     * is the default schema.
     **/
    @get:Input
    @get:Optional
    val outputDefaultSchema: Property<Boolean> = project.objects.property()

    /**
     * Specifies the file to write output for the commands that write it.
     **/
    @get:Input
    @get:Optional
    val outputFile: Property<String> = project.objects.property()

    /**
     * Uses the names as schemaName instead of the real names on diffChangeLog and generateChangeLog commands.
     **/
    @get:Input
    @get:Optional
    val outputSchemasAs: Property<String> = project.objects.property()

    /**
     * Forces overwriting the generated changelog/SQL files.
     **/
    @get:Input
    @get:Optional
    val overwriteOutputFile: Property<Boolean> = project.objects.property()

    /**
     * Prompts if there are non-localhost databases. Default value is: false.
     **/
    @get:Input
    @get:Optional
    val promptForNonLocalDatabase: Property<Boolean> = project.objects.property()

    /**
     * Specifies custom properties implementation to use.
     **/
    @get:Input
    @get:Optional
    val propertyProviderClass: Property<String> = project.objects.property()

    /**
     * Specifies the reference database catalog to use.
     **/
    @get:Input
    @get:Optional
    val referenceDefaultCatalogName: Property<String> = project.objects.property()

    /**
     * Specifies the reference database schema to use.
     **/
    @get:Input
    @get:Optional
    val referenceDefaultSchemaName: Property<String> = project.objects.property()

    /**
     * Specifies a comma-separated list of reference database schemas from which to include objects when executing a
     * command, such as snapshot, generateChangeLog, or diffChangeLog. It is required when you are referencing
     * multiple schemas in a command.
     **/
    @get:Input
    @get:Optional
    val referenceSchemas: Property<String> = project.objects.property()

    /**
     * Specifies the path to a rollback script. If you perform the rollback, the contents of this script will be used
     * to do the rollback rather than what is included in the changelog rollback logic.
     **/
    @get:Input
    @get:Optional
    val rollbackScript: Property<String> = project.objects.property()

    /**
     * Specifies a comma-separated list of database schemas from which to include objects when executing a command,
     * such as snapshot, generateChangeLog, or diffChangeLog. It is required when you are referencing multiple
     * schemas in a command.
     **/
    @get:Input
    @get:Optional
    val schemas: Property<String> = project.objects.property()

    /**
     * Specifies the file format when you run the snapshot or snapshotReference command.
     **/
    @get:Input
    @get:Optional
    val snapshotFormat: Property<String> = project.objects.property()

    /**
     * Specifies the file where SQL statements are stored.
     **/
    @get:Input
    @get:Optional
    val sqlFile: Property<String> = project.objects.property()

    /**
     * Defines whether Liquibase will fail with a validation error if any unknown or inapplicable properties are
     * specified in the liquibase.properties file. In this case, the value should be set to true. Default value
     * is: false.
     **/
    @get:Input
    @get:Optional
    val strict: Property<Boolean> = project.objects.property()

    @TaskAction
    fun generatePropertyFile() {
        outputPropertiesFile.get()
            .apply { parentFile.mkdirs() }
            .writeText {
                if (driver.isPresent) if (driver.isPresent) appendLine("driver=${driver.get()}")
                if (referenceUrl.isPresent) appendLine("referenceUrl=${referenceUrl.get()}")
                if (username.isPresent) appendLine("username=${username.get()}")
                if (password.isPresent) appendLine("password=${password.get()}")
                if (referenceDriver.isPresent) appendLine("referenceDriver=${referenceDriver.get()}")
                if (url.isPresent) appendLine("url=${url.get()}")
                if (referenceUsername.isPresent) appendLine("referenceUsername=${referenceUsername.get()}")
                if (referencePassword.isPresent) appendLine("referencePassword=${referencePassword.get()}")
                if (liquibaseProLicenseKey.isPresent) appendLine("liquibaseProLicenseKey=${liquibaseProLicenseKey.get()}")
                if (classpath.isPresent) appendLine("classpath=${classpath.get()}")
                if (changeExecListenerClass.isPresent) appendLine("changeExecListenerClass=${changeExecListenerClass.get()}")
                if (changeExecListenerPropertiesFile.isPresent) appendLine("changeExecListenerPropertiesFile=${changeExecListenerPropertiesFile.get()}")
                if (changeSetAuthor.isPresent) appendLine("changeSetAuthor=${changeSetAuthor.get()}")
                if (changeSetContext.isPresent) appendLine("changeSetContext=${changeSetContext.get()}")
                if (contexts.isPresent) appendLine("contexts=${contexts.get()}")
                if (currentDateTimeFunction.isPresent) appendLine("currentDateTimeFunction=${currentDateTimeFunction.get()}")
                if (databaseChangeLogLockTableName.isPresent) appendLine("databaseChangeLogLockTableName=${databaseChangeLogLockTableName.get()}")
                if (databaseChangeLogTableName.isPresent) appendLine("databaseChangeLogTableName=${databaseChangeLogTableName.get()}")
                if (databaseChangeLogTablespaceName.isPresent) appendLine("databaseChangeLogTablespaceName=${databaseChangeLogTablespaceName.get()}")
                if (databaseClass.isPresent) appendLine("databaseClass=${databaseClass.get()}")
                if (dataOutputDirectory.isPresent) appendLine("dataOutputDirectory=${dataOutputDirectory.get()}")
                if (defaultCatalogName.isPresent) appendLine("defaultCatalogName=${defaultCatalogName.get()}")
                if (defaultSchemaName.isPresent) appendLine("defaultSchemaName=${defaultSchemaName.get()}")
                if (delimiter.isPresent) appendLine("delimiter=${delimiter.get()}")
                if (diffTypes.isPresent) appendLine("diffTypes=${diffTypes.get()}")
                if (driverPropertiesFile.isPresent) appendLine("driverPropertiesFile=${driverPropertiesFile.get()}")
                if (excludeObjects.isPresent) appendLine("excludeObjects=${excludeObjects.get()}")
                if (includeCatalog.isPresent) appendLine("includeCatalog=${includeCatalog.get()}")
                if (includeObjects.isPresent) appendLine("includeObjects=${includeObjects.get()}")
                if (includeSchema.isPresent) appendLine("includeSchema=${includeSchema.get()}")
                if (includeSystemClasspath.isPresent) appendLine("includeSystemClasspath=${includeSystemClasspath.get()}")
                if (includeTablespace.isPresent) appendLine("includeTablespace=${includeTablespace.get()}")
                if (labels.isPresent) appendLine("labels=${labels.get()}")
                if (liquibaseCatalogName.isPresent) appendLine("liquibaseCatalogName=${liquibaseCatalogName.get()}")
                if (liquibaseHubApiKey.isPresent) appendLine("liquibaseHubApiKey=${liquibaseHubApiKey.get()}")
                if (liquibaseHubConnectionId.isPresent) appendLine("liquibaseHubConnectionId=${liquibaseHubConnectionId.get()}")
                if (liquibaseHubProjectId.isPresent) appendLine("liquibaseHubProjectId=${liquibaseHubProjectId.get()}")
                if (liquibaseHubUrl.isPresent) appendLine("liquibaseHubUrl=${liquibaseHubUrl.get()}")
                if (liquibaseSchemaName.isPresent) appendLine("liquibaseSchemaName=${liquibaseSchemaName.get()}")
                if (logFile.isPresent) appendLine("logFile=${logFile.get()}")
                if (logLevel.isPresent) appendLine("logLevel=${logLevel.get()}")
                if (outputDefaultCatalog.isPresent) appendLine("outputDefaultCatalog=${outputDefaultCatalog.get()}")
                if (outputDefaultSchema.isPresent) appendLine("outputDefaultSchema=${outputDefaultSchema.get()}")
                if (outputFile.isPresent) appendLine("outputFile=${outputFile.get()}")
                if (outputSchemasAs.isPresent) appendLine("outputSchemasAs=${outputSchemasAs.get()}")
                if (overwriteOutputFile.isPresent) appendLine("overwriteOutputFile=${overwriteOutputFile.get()}")
                if (promptForNonLocalDatabase.isPresent) appendLine("promptForNonLocalDatabase=${promptForNonLocalDatabase.get()}")
                if (propertyProviderClass.isPresent) appendLine("propertyProviderClass=${propertyProviderClass.get()}")
                if (referenceDefaultCatalogName.isPresent) appendLine("referenceDefaultCatalogName=${referenceDefaultCatalogName.get()}")
                if (referenceDefaultSchemaName.isPresent) appendLine("referenceDefaultSchemaName=${referenceDefaultSchemaName.get()}")
                if (referenceSchemas.isPresent) appendLine("referenceSchemas=${referenceSchemas.get()}")
                if (rollbackScript.isPresent) appendLine("rollbackScript=${rollbackScript.get()}")
                if (schemas.isPresent) appendLine("schemas=${schemas.get()}")
                if (snapshotFormat.isPresent) appendLine("snapshotFormat=${snapshotFormat.get()}")
                if (sqlFile.isPresent) appendLine("sqlFile=${sqlFile.get()}")
                if (strict.isPresent) appendLine("strict=${strict.get()}")
                if (changeLogFile.isPresent) appendLine("changeLogFile=${changeLogFile.get()}")
            }
    }

}