package com.squaredcandy.io.db.smartlight

import com.squaredcandy.io.db.ChangeType
import com.squaredcandy.europa.model.SmartLight
import kotlinx.coroutines.flow.Flow

interface SmartLightDatabaseInterface {
    suspend fun getAllSmartLights(): List<SmartLight>
    suspend fun upsertSmartLight(smartLight: SmartLight): Boolean
    suspend fun getSmartLight(macAddress: String): SmartLight?
    suspend fun removeSmartLight(macAddress: String): Boolean
    fun getOnSmartLightChanged(macAddress: String): Flow<ChangeType<SmartLight>>
    fun closeDatabase()
}