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
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.format.DateTimeFormatter

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

    override suspend fun removeSmartLight(macAddress: String): Boolean {
        val smartLight = suspendedTransaction {
            SmartLightEntity.find { SmartLightSchema.macAddress eq macAddress }.firstOrNull()
        } ?: return false
        transaction {
            smartLight.delete()
        }
        return true
    }

    override fun closeDatabase() {
        TransactionManager.closeAndUnregister(database)
        closed = true
    }

    private suspend fun insertSmartLight(smartLight: SmartLight): Boolean {
        val smartLightEntity = suspendedTransaction {
            SmartLightEntity.new {
                name = smartLight.name
                macAddress = smartLight.macAddress
                created = smartLight.created.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                lastUpdated = smartLight.lastUpdated.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            }
        }

        insertSmartLightData(smartLight.smartLightData, smartLightEntity.id)

        return true
    }

    private suspend fun updateSmartLight(entity: SmartLightEntity, smartLight: SmartLight): Boolean {
        val currentList = suspendedTransaction {
            entity.data.map { it }
        }
        // Update Smart Light
        suspendedTransaction {
            entity.name = smartLight.name
            entity.lastUpdated = smartLight.lastUpdated.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        }

        insertSmartLightData(smartLight.smartLightData.filterNot { data ->
            currentList.any { it.timestamp == data.timestamp.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) }
        }, entity.id)

        return true
    }

    private suspend fun insertSmartLightData(smartLightData: List<SmartLightData>, entityId: EntityID<Int>) {
        smartLightData.forEach { data ->
            val smartLightDataEntity = suspendedTransaction {
                SmartLightDataEntity.new {
                    smartLight = entityId
                    timestamp = data.timestamp.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    ipAddress = data.ipAddress
                    isOn = data.isOn
                }
            }

            data.capabilities.forEach { capability ->
                when(capability) {
                    is SmartLightCapability.SmartLightColor.SmartLightHSB -> {
                        val smartLightColor = suspendedTransaction {
                            SmartLightCapabilityColorEntity.new {
                                hue = capability.hue
                                saturation = capability.saturation
                                brightness = capability.brightness
                            }
                        }
                        suspendedTransaction {
                            smartLightDataEntity.color = smartLightColor
                        }
                    }
                    is SmartLightCapability.SmartLightColor.SmartLightKelvin -> {
                        val smartLightColor = suspendedTransaction {
                            SmartLightCapabilityColorEntity.new {
                                kelvin = capability.kelvin
                                brightness = capability.brightness
                            }
                        }
                        suspendedTransaction {
                            smartLightDataEntity.color = smartLightColor
                        }
                    }
                    is SmartLightCapability.SmartLightLocation -> {
                        val smartLightLocation = suspendedTransaction {
                            SmartLightCapabilityLocationEntity.new {
                                location = capability.location
                            }
                        }
                        suspendedTransaction {
                            smartLightDataEntity.location = smartLightLocation
                        }
                    }
                }
            }
        }
    }

    private suspend fun <T> suspendedTransaction(block: suspend Transaction.() -> T): T
        = newSuspendedTransaction(
        context = Dispatchers.IO,
        db = database,
        statement = block
    )
}