package com.squaredcandy.db.smartlight.model.schema

import org.jetbrains.exposed.dao.id.UUIDTable

internal object SmartLightCapabilityColorSchema : UUIDTable(name = "smart_light_capability_color") {
    val hue = float("hue").nullable()
    val saturation = float("saturation").nullable()
    val brightness = float("brightness")
    val kelvin = integer("kelvin").nullable()
}