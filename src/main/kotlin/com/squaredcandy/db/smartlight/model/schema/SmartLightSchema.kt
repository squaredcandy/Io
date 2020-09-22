package com.squaredcandy.db.smartlight.model.schema

import org.jetbrains.exposed.dao.id.IntIdTable
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

internal object SmartLightSchema : IntIdTable(name = "smart_light") {
    val name = varchar("name", length = 50)
    val macAddress = varchar("mac_address", length = 20)
    val created = varchar("created", length = 35).default(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
    val lastUpdated = varchar("last_updated", length = 35).default(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
}