package com.localhost.core.model

import kotlinx.serialization.Serializable

enum class DatabaseEngine(val displayName: String, val defaultPort: Int, val defaultUser: String) {
    MYSQL("MySQL", 3306, "root"),
    POSTGRESQL("PostgreSQL", 5432, "postgres")
}

@Serializable
data class DatabaseConfig(
    val engine: DatabaseEngine = DatabaseEngine.MYSQL,
    val host: String = "127.0.0.1",
    val port: Int = 3306,
    val user: String = "root",
    val password: String = "",
    val database: String = ""
)

data class ColumnInfo(
    val name: String,
    val type: String,
    val isNullable: Boolean = true,
    val isPrimaryKey: Boolean = false,
    val defaultValue: String? = null
)

data class TableData(
    val tableName: String,
    val columns: List<String>,
    val rows: List<List<String>>
)

data class QueryResult(
    val isSuccess: Boolean,
    val executionTimeMs: Long = 0L,
    val affectedRows: Int = 0,
    val columns: List<String> = emptyList(),
    val rows: List<List<String>> = emptyList(),
    val errorMessage: String? = null
)

data class DatabaseUser(
    val username: String,
    val host: String = "%",
    val privileges: List<String> = emptyList()
)
