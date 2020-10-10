package com.squaredcandy.io.db.smartlight.model.entity

import com.squaredcandy.io.db.smartlight.model.schema.SmartLightCapabilityLocationSchema
import com.squaredcandy.europa.model.SmartLightCapability
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.UUID

internal class SmartLightCapabilityLocationEntity(id: EntityID<UUID>): UUIDEntity(id) {
    companion object : UUIDEntityClass<SmartLightCapabilityLocationEntity>(SmartLightCapabilityLocationSchema)
    var location by SmartLightCapabilityLocationSchema.location
}

internal fun SmartLightCapabilityLocationEntity.toSmartLightLocation(): SmartLightCapability.SmartLightLocation {
    return SmartLightCapability.SmartLightLocation(
        location = location
    )
}