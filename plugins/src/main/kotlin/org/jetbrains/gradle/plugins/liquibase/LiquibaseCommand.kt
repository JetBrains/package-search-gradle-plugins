package org.jetbrains.gradle.plugins.liquibase

enum class LiquibaseCommand(val command: String, val description: String, val requiresValue: Boolean = false) {
    CHANGELOG_SYNC("changelogSync", "Mark all changes as executed in the database.", false),
    CHANGELOG_SYNC_SQL("changelogSyncSQL", "Writes SQL to mark all changes as executed in the database to STDOUT.", false),
    CLEAR_CHECKSUMS(
        "clearChecksums",
        "Removes all saved checksums from the database. On next run checksums will be recomputed.  Useful for 'MD5Sum Check Failed' errors.",
        false
    ),
    DIFF("diff", "Writes description of differences to standard out.", false),
    DIFF_CHANGE_LOG("diffChangeLog", "Writes Change Log to update the database to the reference database to standard out", false),
    DROP_ALL(
        "dropAll",
        "Drops all database objects owned by the user. Note that functions, procedures and packages are not dropped (Liquibase limitation)",
        false
    ),
    FUTURE_ROLLBACK_SQL(
        "futureRollbackSQL",
        "Writes SQL to roll back the database to the current state after the changes in the changeslog have been applied.",
        false
    ),
    GENERATE_CHANGELOG("generateChangelog", "Writes Change Log groovy to copy the current state of the database to standard out.", false),
    HISTORY("history", "lists out all your deploymentIds and all changesets associated with each deploymentId.", false),
    LIST_LOCKS("listLocks", "Lists who currently has locks on the database changelog.", false),
    MARK_NEXT_CHANGESET_RAN("markNextChangesetRan", "Mark the next change set as executed in the database.", false),
    MARK_NEXT_CHANGESET_RAN_SQL(
        "markNextChangesetRanSQL",
        "Writes SQL to mark the next change set as executed in the database to STDOUT.",
        false
    ),
    RELEASE_LOCKS("releaseLocks", "Releases all locks on the database changelog.", false),
    SNAPSHOT("snapshot", "Writes the current state of the database to standard out", false),
    SNAPSHOT_REFERENCE("snapshotReference", "Writes the current state of the referenceUrl database to standard out", false),
    STATUS("status", "Outputs count (list if liquibaseCommandValue is '--verbose') of unrun change sets.", false),
    UNEXPECTED_CHANGE_SETS(
        "unexpectedChangeSets",
        "Outputs count (list if liquibaseCommandValue is '--verbose') of changesets run in the database that do not exist in the changelog.",
        false
    ),
    UPDATE("update", "Updates the database to the current version.", false),
    UPDATE_SQL("updateSQL", "Writes SQL to update the database to the current version to STDOUT.", false),
    UPDATE_TESTING_ROLLBACK("updateTestingRollback", "Updates the database, then rolls back changes before updating again.", false),
    VALIDATE("validate", "Checks the changelog for errors.", false),
    CALCULATE_CHECK_SUM(
        "calculateCheckSum",
        "Calculates and prints a checksum for the <liquibaseCommandValue> changeset with the given id in the format filepath::id::author.",
        true
    ),
    DB_DOC(
        "dbDoc",
        "Generates Javadoc-like documentation based on current database and change log to the <liquibaseCommandValue> directory.",
        true
    ),
    EXECUTE_SQL(
        "executeSql",
        "Executes SQL in the database given in <liquibaseCommandValue> in this format: -PliquibaseCommandValue='--sql=select 1' or -PliquibaseCommandValue='--sqlFile=myfile.sql'",
        true
    ),
    FUTURE_ROLLBACK_COUNT_SQL(
        "futureRollbackCountSQL",
        "Writes SQL to roll back <liquibaseCommandValue> changes the database after the changes in the changelog have been applied.",
        true
    ),
    FUTURE_ROLLBACK_FROM_TAG_SQL(
        "futureRollbackFromTagSQL",
        "Writes (to standard out) the SQL to roll back the database to its current state after the changes up to the <liquibaseCommandValue> tag have been	applied",
        true
    ),
    ROLLBACK("rollback", "Rolls back the database to the state it was in when the <liquibaseCommandValue> tag was applied.", true),
    ROLLBACK_COUNT("rollbackCount", "Rolls back the last <liquibaseCommandValue> change sets.", true),
    ROLLBACK_COUNT_SQL("rollbackCountSQL", "Writes SQL to roll back the last <liquibaseCommandValue> change sets to STDOUT.", true),
    ROLLBACK_SQL(
        "rollbackSQL",
        "Writes SQL to roll back the database to the state it was in when the <liquibaseCommandValue> tag was applied to STDOUT.",
        true
    ),
    ROLLBACK_TO_DATE("rollbackToDate", "Rolls back the database to the state it was in at the <liquibaseCommandValue> date/time.", true),
    ROLLBACK_TO_DATE_SQL(
        "rollbackToDateSQL",
        "Writes SQL to roll back the database to the state it was in at the <liquibaseCommandValue> date/time to STDOUT.",
        true
    ),
    TAG("tag", "Tags the current database state with <liquibaseCommandValue> for future rollback.", true),
    TAG_EXISTS("tagExists", "Checks whether the tag given in <liquibaseCommandValue> is already existing.", true),
    UPDATE_COUNT("updateCount", "Applies the next <liquibaseCommandValue> change sets.", true),
    UPDATE_COUNT_SQL("updateCountSql", "Writes SQL to apply the next <liquibaseCommandValue> change sets to STDOUT.", true),
    UPDATE_TO_TAG("updateToTag", "Updates the database to the changeSet with the <liquibaseCommandValue> tag", true),
    UPDATE_TO_TAG_SQL(
        "updateToTagSQL",
        "Writes (to standard out) the SQL to update to the changeSet with the <liquibaseCommandValue> tag",
        true
    );

    operator fun component1() = command
    operator fun component2() = description
    operator fun component3() = requiresValue
}
