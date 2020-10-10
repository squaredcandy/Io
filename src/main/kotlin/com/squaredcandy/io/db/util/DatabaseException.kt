package com.squaredcandy.io.db.util

/**
 * A exception which holds a [DatabaseErrorType] to identify which error had occurred when interacting with the database
 *
 * @param type Type of error occurred
 * @param cause Pass along the parameter from [Throwable]
 */
open class DatabaseException(val type: DatabaseErrorType, override val cause: Throwable? = null)
    : Throwable(type.message, cause) {

    override fun equals(other: Any?): Boolean {
        return if(other !is DatabaseException) super.equals(other)
        else (type == other.type && cause == other.cause)
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + (cause?.hashCode() ?: 0)
        return result
    }
}