package com.squaredcandy.io.db.smartlight.model.entity

import com.squaredcandy.io.db.smartlight.model.schema.SmartLightCapabilityColorSchema
import com.squaredcandy.europa.model.SmartLightCapability
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.UUID

internal class SmartLightCapabilityColorEntity(id: EntityID<UUID>): UUIDEntity(id) {
    companion object : UUIDEntityClass<SmartLightCapabilityColorEntity>(SmartLightCapabilityColorSchema)
    var hue by SmartLightCapabilityColorSchema.hue
    var saturation by SmartLightCapabilityColorSchema.saturation
    var brightness by SmartLightCapabilityColorSchema.brightness
    var kelvin by SmartLightCapabilityColorSchema.kelvin
}

internal fun SmartLightCapabilityColorEntity.toSmartLightColor(): SmartLightCapability.SmartLightColor {
    return if(kelvin != null) {
        SmartLightCapability.SmartLightColor.SmartLightKelvin(
            kelvin = kelvin!!,
            brightness = brightness
        )
    } else {
        SmartLightCapability.SmartLightColor.SmartLightHSB(
            hue = hue!!,
            saturation = saturation!!,
            brightness = brightness,
        )
    }
}