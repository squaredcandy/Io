package com.squaredcandy.io.db.smartlight

import com.squaredcandy.io.db.util.ChangeType
import com.squaredcandy.io.db.util.DatabaseException
import com.squaredcandy.io.db.util.DatabaseErrorType
import com.squaredcandy.europa.model.SmartLight
import com.squaredcandy.europa.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Interface for interacting with the smart lights database
 */
interface SmartLightDatabase {
    /**
     * Get all the smart lights in the database
     *
     * @return A [Result.Success] of all the smart lights in the database
     * @exception DatabaseException in [Result.Failure] with type [DatabaseErrorType.CLOSED] if the database is closed
     */
    suspend fun getAllSmartLights(): Result<List<SmartLight>>

    /**
     * Insert or update a smart light in the database
     *
     * @param smartLight Smart light to insert or update
     * @return A [Result.Success]
     * @exception DatabaseException in [Result.Failure] with type [DatabaseErrorType.NO_CHANGE] if not inserted
     * @exception DatabaseException in [Result.Failure] with type [DatabaseErrorType.CLOSED] if the database is closed
     */
    suspend fun upsertSmartLight(smartLight: SmartLight): Result<Unit>

    /**
     * Get a smart light with a specific [macAddress]
     *
     * @param macAddress Mac address to query for
     * @return A [Result.Success] containing the smart light
     * @exception DatabaseException in [Result.Failure] with type [DatabaseErrorType.NOT_FOUND] if no smart light was found
     * @exception DatabaseException in [Result.Failure] with type [DatabaseErrorType.CLOSED] if the database is closed
     */
    suspend fun getSmartLight(macAddress: String): Result<SmartLight>

    /**
     * Remove a smart light and all accompanying data with a specific [macAddress]
     *
     * @param macAddress Mac address to query for
     * @return A [Result.Success] containing the removed smart light
     * @exception DatabaseException in [Result.Failure] with type [DatabaseErrorType.NOT_FOUND] if no smart light was found
     * @exception DatabaseException in [Result.Failure] with type [DatabaseErrorType.CLOSED] if the database is closed
     */
    suspend fun removeSmartLight(macAddress: String): Result<SmartLight>

    /**
     * Get a flow of events which detail what has happened to the specified smart light matching the mac address
     *
     * @param macAddress Mac address to filter for
     * @return A flow of smart light change type events or an empty flow if the database is closed
     */
    fun getOnSmartLightChanged(macAddress: String): Flow<ChangeType<SmartLight>>

    /**
     * Closes connection to the current database
     */
    fun closeDatabase()
}