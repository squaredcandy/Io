package com.squaredcandy.io.db.util

/**
 * Sealed class identifying the types of changes that occurred to [T]
 */
sealed class ChangeType<out T> {
    /**
     * Class identifying that an item of type [T] as been inserted
     *
     * @param item Item that has been inserted
     */
    data class Inserted<T>(val item: T): ChangeType<T>()

    /**
     * Class identifying that an item of type [T] has been updated
     *
     * @param item Item that has been updated
     */
    data class Updated<T>(val item: T): ChangeType<T>()

    /**
     * Class identifying that an item has been removed
     */
    object Removed : ChangeType<Nothing>()
}