package com.squaredcandy.io.db

sealed class ChangeType<out T> {
    data class Inserted<T>(val item: T): ChangeType<T>()
    data class Updated<T>(val item: T): ChangeType<T>()
    object Removed : ChangeType<Nothing>()
}