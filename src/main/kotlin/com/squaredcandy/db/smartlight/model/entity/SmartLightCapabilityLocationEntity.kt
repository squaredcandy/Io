package com.squaredcandy.db.smartlight.model.entity

import com.squaredcandy.db.smartlight.model.schema.SmartLightCapabilityLocationSchema
import com.squaredcandy.europa.model.SmartLightCapability
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

internal class SmartLightCapabilityLocationEntity(id: EntityID<Int>): IntEntity(id) {
    companion object : IntEntityClass<SmartLightCapabilityLocationEntity>(SmartLightCapabilityLocationSchema)
    var location by SmartLightCapabilityLocationSchema.location
}

internal fun SmartLightCapabilityLocationEntity.toSmartLightLocation(): SmartLightCapability.SmartLightLocation {
    return SmartLightCapability.SmartLightLocation(
        location = location
    )
}