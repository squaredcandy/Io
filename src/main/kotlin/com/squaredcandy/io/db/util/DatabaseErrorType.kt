package com.squaredcandy.io.db.util

/**
 * Database error types that can occur when interacting with the database
 *
 * @property message Message to pass along to [Throwable]
 */
enum class DatabaseErrorType(val message: String) {
    /**
     * Used when inserting or updating a database where the value is not practically different to the value currently
     * inside the database
     */
    NO_CHANGE("Value is not different to the current value"),

    /**
     * Used when a database query returns no result
     */
    NOT_FOUND("No results where found based on this query"),

    /**
     * The current database is closed
     */
    CLOSED("This database is currently closed"),
}