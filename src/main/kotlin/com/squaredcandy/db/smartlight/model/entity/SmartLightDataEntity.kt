package com.squaredcandy.db.smartlight.model.entity

import com.squaredcandy.db.smartlight.model.schema.SmartLightDataSchema
import com.squaredcandy.europa.model.SmartLightCapability
import com.squaredcandy.europa.model.SmartLightData
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

internal class SmartLightDataEntity(id: EntityID<Int>): IntEntity(id) {
    companion object : IntEntityClass<SmartLightDataEntity>(SmartLightDataSchema)
    var smartLight by SmartLightDataSchema.smartLightRef
    var timestamp: OffsetDateTime by SmartLightDataSchema.timestamp.transform(
        { it.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) },
        { OffsetDateTime.parse(it) }
    )
    var ipAddress by SmartLightDataSchema.ipAddress
    var isOn by SmartLightDataSchema.isOn
    var color by SmartLightCapabilityColorEntity optionalReferencedOn  SmartLightDataSchema.colorRef
    var location by SmartLightCapabilityLocationEntity optionalReferencedOn SmartLightDataSchema.locationRef

    override fun delete() {
        transaction { color }?.delete()
        transaction { location }?.delete()
        super.delete()
    }
}

internal fun SmartLightDataEntity.toSmartLightData(): SmartLightData {
    val color = transaction { color?.toSmartLightColor() }
    val location = transaction { location?.toSmartLightLocation() }
    val capabilities = mutableListOf<SmartLightCapability>()
    if(color != null) capabilities.add(color)
    if(location != null) capabilities.add(location)
    return SmartLightData(
        timestamp = timestamp,
        ipAddress = ipAddress,
        isOn = isOn,
        capabilities = capabilities
    )
}