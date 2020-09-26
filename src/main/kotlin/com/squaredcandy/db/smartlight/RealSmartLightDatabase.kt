package com.squaredcandy.db.smartlight

import com.squaredcandy.db.smartlight.model.entity.*
import com.squaredcandy.db.smartlight.model.schema.SmartLightCapabilityColorSchema
import com.squaredcandy.db.smartlight.model.schema.SmartLightCapabilityLocationSchema
import com.squaredcandy.db.smartlight.model.schema.SmartLightDataSchema
import com.squaredcandy.db.smartlight.model.schema.SmartLightSchema
import kotlinx.coroutines.Dispatchers
import com.squaredcandy.europa.model.SmartLight
import com.squaredcandy.europa.model.SmartLightCapability
import com.squaredcandy.europa.model.SmartLightData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

internal class RealSmartLightDatabase(
    private val database: Database
) : SmartLightDatabaseInterface {

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
        return suspendedTransaction {
            SmartLightEntity.all().map { it.toSmartLight() }
        }
    }

    override suspend fun upsertSmartLight(smartLight: SmartLight): Boolean {
        val entity = suspendedTransaction {
            SmartLightEntity.find { SmartLightSchema.macAddress eq smartLight.macAddress }.firstOrNull()
        }
        return if(entity != null) updateSmartLight(entity, smartLight) else insertSmartLight(smartLight)
    }

    override suspend fun getSmartLight(macAddress: String): SmartLight? {
        return suspendedTransaction {
            SmartLightEntity.find { SmartLightSchema.macAddress eq macAddress }.firstOrNull()
        }?.toSmartLight()
    }

    override fun getOnSmartLightUpdated(macAddress: String): Flow<SmartLight> {
        return callbackFlow {
            val hook = hook@ { entityChange: EntityChange ->
                if(
                    entityChange.entityClass != SmartLightEntity ||
                    entityChange.changeType == EntityChangeType.Removed
                ) return@hook
                val smartLightEntity = entityChange.toEntity(SmartLightEntity)
                if(smartLightEntity != null && smartLightEntity.macAddress == macAddress) {
                    offer(smartLightEntity.toSmartLight())
                }
            }
            EntityHook.subscribe(hook)
            awaitClose { EntityHook.unsubscribe(hook) }
        }
    }

    override suspend fun removeSmartLight(macAddress: String): Boolean {
        val smartLight = suspendedTransaction {
            SmartLightEntity.find { SmartLightSchema.macAddress eq macAddress }.firstOrNull()
        } ?: return false
        transaction {
            smartLight.delete()
            registerChange(SmartLightEntity, smartLight.id, EntityChangeType.Removed)
        }
        return true
    }

    override fun closeDatabase() {
        TransactionManager.closeAndUnregister(database)
        closed = true
    }

    private suspend fun insertSmartLight(smartLight: SmartLight): Boolean {
        suspendedTransaction {
            val smartLightEntity = SmartLightEntity.new {
                name = smartLight.name
                macAddress = smartLight.macAddress
                created = smartLight.created
                lastUpdated = smartLight.lastUpdated
            }
            insertSmartLightData(smartLight.smartLightData, smartLightEntity.id)
        }
        return true
    }

    private suspend fun updateSmartLight(entity: SmartLightEntity, smartLight: SmartLight): Boolean {
        val currentSmartLight = entity.toSmartLight()

        val filter = smartLight.smartLightData.filterNot { data ->
            currentSmartLight.smartLightData.any { it.timestamp == data.timestamp }
        }
        return if(currentSmartLight.name != smartLight.name || filter.isNotEmpty()) {
            suspendedTransaction {
                entity.name = smartLight.name
                entity.lastUpdated = smartLight.lastUpdated

                insertSmartLightData(filter, entity.id)
            }
            suspendedTransaction {
                registerChange(SmartLightEntity, entity.id, EntityChangeType.Updated)
            }
            true
        } else false
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