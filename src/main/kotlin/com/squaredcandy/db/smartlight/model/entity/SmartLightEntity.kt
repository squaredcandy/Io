package com.squaredcandy.db.smartlight.model.entity

import com.squaredcandy.db.smartlight.model.schema.SmartLightDataSchema
import com.squaredcandy.db.smartlight.model.schema.SmartLightSchema
import com.squaredcandy.europa.model.SmartLight
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

internal class SmartLightEntity(id: EntityID<Int>): IntEntity(id) {
    companion object : IntEntityClass<SmartLightEntity>(SmartLightSchema)
    var name by SmartLightSchema.name
    var macAddress by SmartLightSchema.macAddress
    var created: OffsetDateTime by SmartLightSchema.created.transform(
        { it.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) },
        { OffsetDateTime.parse(it) }
    )
    var lastUpdated: OffsetDateTime by SmartLightSchema.lastUpdated.transform(
        { it.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) },
        { OffsetDateTime.parse(it) }
    )
    val data by SmartLightDataEntity referrersOn SmartLightDataSchema.smartLightRef

    override fun delete() {
        transaction { data }.forEach { it.delete() }
        super.delete()
    }
}

internal fun SmartLightEntity.toSmartLight(): SmartLight {
    return SmartLight(
        name = name,
        macAddress = macAddress,
        created = created,
        lastUpdated = lastUpdated,
        transaction { data.map { it.toSmartLightData() } }
    )
}