package com.squaredcandy.io.db.smartlight

import com.squaredcandy.io.db.util.ChangeType
import com.squaredcandy.io.db.smartlight.model.entity.*
import com.squaredcandy.io.db.smartlight.model.schema.SmartLightCapabilityColorSchema
import com.squaredcandy.io.db.smartlight.model.schema.SmartLightCapabilityLocationSchema
import com.squaredcandy.io.db.smartlight.model.schema.SmartLightDataSchema
import com.squaredcandy.io.db.smartlight.model.schema.SmartLightSchema
import kotlinx.coroutines.Dispatchers
import com.squaredcandy.europa.model.SmartLight
import com.squaredcandy.europa.model.SmartLightCapability
import com.squaredcandy.europa.model.SmartLightData
import com.squaredcandy.europa.util.Result
import com.squaredcandy.europa.util.getResultSuspended
import com.squaredcandy.io.db.util.DatabaseErrorType
import com.squaredcandy.io.db.util.DatabaseException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

internal class RealSmartLightDatabase(
    private val database: Database
) : SmartLightDatabase {

    private var closed: Boolean = false

    init {
        transaction {
            addLogger(StdOutSqlLogger)

            SchemaUtils.createMissingTablesAndColumns(
                SmartLightSchema,
                SmartLightDataSchema,
                SmartLightCapabilityColorSchema,
                SmartLightCapabilityLocationSchema,
            )
        }
        closed = false
    }

    override suspend fun getAllSmartLights(): List<SmartLight> {
        if(closed) return emptyList()
        return suspendedTransaction {
            SmartLightEntity.all().map { it.toSmartLight() }
        }
    }

    override suspend fun upsertSmartLight(smartLight: SmartLight): Result<SmartLight> {
        if(closed) return Result.Failure(DatabaseException(DatabaseErrorType.CLOSED))
        val entity = suspendedTransaction {
            SmartLightEntity.find { SmartLightSchema.macAddress eq smartLight.macAddress }.firstOrNull()
        }
        return if(entity != null) updateSmartLight(entity, smartLight) else insertSmartLight(smartLight)
    }

    override suspend fun getSmartLight(macAddress: String): Result<SmartLight> {
        if(closed) return Result.Failure(DatabaseException(DatabaseErrorType.CLOSED))
        val smartLight = suspendedTransaction {
            SmartLightEntity.find { SmartLightSchema.macAddress eq macAddress }.firstOrNull()
        }?.toSmartLight()
        return if(smartLight != null) Result.Success(smartLight)
        else Result.Failure(DatabaseException(DatabaseErrorType.NOT_FOUND))
    }

    @ExperimentalCoroutinesApi
    override fun getOnSmartLightChanged(macAddress: String): Flow<ChangeType<SmartLight>> {
        if(closed) return emptyFlow()
        return callbackFlow {
            val hook = hook@ { entityChange: EntityChange ->
                if(entityChange.entityClass != SmartLightEntity) return@hook
                when(entityChange.changeType) {
                    EntityChangeType.Created -> {
                        val smartLightEntity = entityChange.toEntity(SmartLightEntity)
                        if(smartLightEntity != null && smartLightEntity.macAddress == macAddress) {
                            offer(ChangeType.Inserted(smartLightEntity.toSmartLight()))
                        }
                    }
                    EntityChangeType.Updated -> {
                        val smartLightEntity = entityChange.toEntity(SmartLightEntity)
                        if(smartLightEntity != null && smartLightEntity.macAddress == macAddress) {
                            offer(ChangeType.Updated(smartLightEntity.toSmartLight()))
                        }
                    }
                    EntityChangeType.Removed -> {
                        offer(ChangeType.Removed)
                    }
                }
            }
            EntityHook.subscribe(hook)
            awaitClose { EntityHook.unsubscribe(hook) }
        }
    }

    override suspend fun removeSmartLight(macAddress: String): Result<SmartLight> {
        if(closed) return Result.Failure(DatabaseException(DatabaseErrorType.CLOSED))
        val smartLight = suspendedTransaction {
            SmartLightEntity.find { SmartLightSchema.macAddress eq macAddress }.firstOrNull()
        } ?: return Result.Failure(DatabaseException(DatabaseErrorType.NOT_FOUND))
        transaction {
            smartLight.delete()
            registerChange(SmartLightEntity, smartLight.id, EntityChangeType.Removed)
        }
        return Result.Success(smartLight.toSmartLight())
    }

    override fun closeDatabase() {
        TransactionManager.closeAndUnregister(database)
        closed = true
    }

    private suspend fun insertSmartLight(smartLight: SmartLight): Result<SmartLight> {
        return getResultSuspended {
            suspendedTransaction {
                val smartLightEntity = SmartLightEntity.new {
                    name = smartLight.name
                    macAddress = smartLight.macAddress
                    created = smartLight.created
                    lastUpdated = smartLight.lastUpdated
                }
                insertSmartLightData(smartLight.smartLightData, smartLightEntity.id)
                smartLight
            }
        }
    }

    private suspend fun updateSmartLight(entity: SmartLightEntity, smartLight: SmartLight): Result<SmartLight> {
        val smartLightData = suspendedTransaction {
            entity.data.map { it.toSmartLightDataWithId() }
                .plus(smartLight.smartLightData.map { null to it })
                .sortedBy { it.second.timestamp }
                .toMutableList()
        }

        val removeList = mutableListOf<UUID>()
        // Ensure we have at least 2 data points
        if(smartLightData.size > 1) {
            var currentIndex = 0
            while(currentIndex < smartLightData.lastIndex) {
                val (currentId, currentData) = smartLightData[currentIndex]
                val (nextId, nextData) = smartLightData[currentIndex + 1]

                // We have the same timestamp or if the data points are the same we keep the one in the db or the first
                // one submitted
                if(
                    currentData.timestamp == nextData.timestamp ||
                    (
                        currentData.ipAddress == nextData.ipAddress &&
                        currentData.isOn == nextData.isOn &&
                        currentData.isConnected == nextData.isConnected &&
                        currentData.capabilities == nextData.capabilities
                    )
                ) {
                    if(currentId == null && nextId == null) {
                        smartLightData.removeAt(currentIndex + 1)
                    } else if (currentId != null && nextId == null) {
                        smartLightData.removeAt(currentIndex + 1)
                    } else if (currentId == null && nextId != null) {
                        smartLightData.removeAt(currentIndex)
                    } else if (currentId != null && nextId != null) {
                        removeList.add(nextId)
                        smartLightData.removeAt(currentIndex + 1)
                    } else {
                        // Something went very wrong
                        return Result.Failure(DatabaseException(DatabaseErrorType.INTERNAL_ERROR))
                    }
                    continue
                }
                currentIndex++
            }
        }

        val nameChanged = entity.name != smartLight.name
        val newData = smartLightData.mapNotNull { (id, data) ->
            if(id != null) null else data
        }
        val dataChanged = newData.isNotEmpty() || removeList.isNotEmpty()
        return if(nameChanged || dataChanged) {
            suspendedTransaction {
                entity.lastUpdated = smartLight.lastUpdated
                if(nameChanged) {
                    entity.name = smartLight.name
                }
                if(dataChanged) {
                    removeList.forEach { SmartLightDataEntity[it].delete() }
                    insertSmartLightData(newData, entity.id)
                }
            }
            // Changing the name triggers an update however changing the data doesn't so we need to do it ourselves
            if(!nameChanged && dataChanged) {
                suspendedTransaction {
                    registerChange(SmartLightEntity, entity.id, EntityChangeType.Updated)
                }
            }
            getSmartLight(smartLight.macAddress)
        } else Result.Failure(DatabaseException(DatabaseErrorType.NO_CHANGE))
    }

    private fun Transaction.insertSmartLightData(smartLightData: List<SmartLightData>, entityId: EntityID<UUID>) {
        smartLightData.forEach { data ->
            val smartLightDataEntity = SmartLightDataEntity.new {
                smartLight = entityId
                timestamp = data.timestamp
                ipAddress = data.ipAddress
                isOn = data.isOn
            }

            data.capabilities.forEach { capability ->
                when(capability) {
                    is SmartLightCapability.SmartLightColor.SmartLightHSB -> {
                        val smartLightColor = capability.toEntity()
                        smartLightDataEntity.color = smartLightColor
                    }
                    is SmartLightCapability.SmartLightColor.SmartLightKelvin -> {
                        val smartLightColor = capability.toEntity()
                        smartLightDataEntity.color = smartLightColor
                    }
                    is SmartLightCapability.SmartLightLocation -> {
                        val smartLightLocation = capability.toEntity()
                        smartLightDataEntity.location = smartLightLocation
                    }
                }
            }
        }
    }

    private fun SmartLightCapability.SmartLightColor.SmartLightHSB.toEntity(): SmartLightCapabilityColorEntity {
        return SmartLightCapabilityColorEntity.new {
            this.hue = this@toEntity.hue
            this.saturation = this@toEntity.saturation
            this.brightness = this@toEntity.brightness
        }
    }

    private fun SmartLightCapability.SmartLightColor.SmartLightKelvin.toEntity(): SmartLightCapabilityColorEntity {
        return SmartLightCapabilityColorEntity.new {
            this.kelvin = this@toEntity.kelvin
            this.brightness = this@toEntity.brightness
        }
    }

    private fun SmartLightCapability.SmartLightLocation.toEntity(): SmartLightCapabilityLocationEntity {
        return SmartLightCapabilityLocationEntity.new {
            this.location = this@toEntity.location
        }
    }

    private suspend fun <T> suspendedTransaction(block: suspend Transaction.() -> T): T
        = newSuspendedTransaction(
        context = Dispatchers.IO,
        db = database,
        statement = block
    )
}