package com.squaredcandy.db.smartlight.model.entity

import com.squaredcandy.db.smartlight.model.schema.SmartLightCapabilityColorSchema
import com.squaredcandy.europa.model.SmartLightCapability
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

internal class SmartLightCapabilityColorEntity(id: EntityID<Int>): IntEntity(id) {
    companion object : IntEntityClass<SmartLightCapabilityColorEntity>(SmartLightCapabilityColorSchema)
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