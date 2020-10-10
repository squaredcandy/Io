package com.squaredcandy.io.db.smartlight.model.schema

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

internal object SmartLightDataSchema : UUIDTable(name = "smart_light_data") {
    val smartLightRef = reference("smart_light", SmartLightSchema)
    val timestamp = varchar("timestamp", length = 35).default(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
    val ipAddress = varchar("ip_address", length = 20).nullable()
    val isOn = bool("is_on")
    val colorRef = optReference("color", SmartLightCapabilityColorSchema, onDelete = ReferenceOption.CASCADE)
    val locationRef = optReference("location", SmartLightCapabilityLocationSchema, onDelete = ReferenceOption.CASCADE)
}