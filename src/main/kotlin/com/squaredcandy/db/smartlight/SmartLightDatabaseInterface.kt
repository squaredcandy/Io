package com.squaredcandy.db.smartlight

import com.squaredcandy.europa.model.SmartLight

interface SmartLightDatabaseInterface {
    suspend fun getAllSmartLights(): List<SmartLight>
    suspend fun upsertSmartLight(smartLight: SmartLight): Boolean
    suspend fun getSmartLight(macAddress: String): SmartLight?
    suspend fun removeSmartLight(macAddress: String): Boolean
    fun closeDatabase()
}