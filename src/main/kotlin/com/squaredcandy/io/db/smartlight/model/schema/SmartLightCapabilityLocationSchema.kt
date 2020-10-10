package com.squaredcandy.io.db.smartlight.model.schema

import org.jetbrains.exposed.dao.id.UUIDTable

internal object SmartLightCapabilityLocationSchema : UUIDTable(name = "smart_light_capability_location") {
    val location = varchar("location", length = 50)
}