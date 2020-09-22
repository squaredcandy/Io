package com.squaredcandy.db.smartlight.model

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.squaredcandy.db.smartlight.RealSmartLightDatabase
import com.squaredcandy.db.smartlight.SmartLightDatabaseInterface
import org.jetbrains.exposed.sql.Database
import javax.sql.DataSource

object DatabaseProvider {

    private fun getDataSource(
        driverClassName: String = "com.impossibl.postgres.jdbc.PGDriver",
        jdbcUrl: String = "jdbc:pgsql://localhost:5432/postgres",
        username: String? = "postgres",
        password: String? = "1qaz",
    ): DataSource {
        val config = HikariConfig().apply {
            this.driverClassName = driverClassName
            this.jdbcUrl = jdbcUrl
            this.username = username
            this.password = password
            maximumPoolSize = 10
            validate()
        }
        return HikariDataSource(config)
    }

    fun getSmartLightDatabase(
        driverClassName: String = "com.impossibl.postgres.jdbc.PGDriver",
        jdbcUrl: String = "jdbc:pgsql://localhost:5432/postgres",
        username: String? = "postgres",
        password: String? = "1qaz",
    ): SmartLightDatabaseInterface {
        val dataSource = getDataSource(
            driverClassName,
            jdbcUrl,
            username,
            password,
        )
        val database = Database.connect(dataSource)
        return RealSmartLightDatabase(database)
    }
}