package com.squaredcandy.db.smartlight.model.schema

import org.jetbrains.exposed.dao.id.IntIdTable

internal object SmartLightCapabilityLocationSchema : IntIdTable(name = "smart_light_capability_location") {
    val location = varchar("location", length = 50)
}