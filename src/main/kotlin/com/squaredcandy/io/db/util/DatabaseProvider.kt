package com.squaredcandy.io.db.util

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.squaredcandy.io.db.smartlight.RealSmartLightDatabase
import com.squaredcandy.io.db.smartlight.SmartLightDatabase
import org.jetbrains.exposed.sql.Database
import javax.sql.DataSource

object DatabaseProvider {

    private var dataSource: DataSource? = null
    private val database by lazy {
        Database.connect(dataSource ?: getDataSource())
    }
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

    /**
     * Gets an interface to the database
     *
     * @param driverClassName Database driver class
     * @param jdbcUrl Connection url address
     * @param username Database username
     * @param password Database password
     * @return
     */
    fun getSmartLightDatabase(
        driverClassName: String = "com.impossibl.postgres.jdbc.PGDriver",
        jdbcUrl: String = "jdbc:pgsql://localhost:5432/postgres",
        username: String? = "postgres",
        password: String? = "1qaz",
    ): SmartLightDatabase {
        dataSource = getDataSource(
            driverClassName,
            jdbcUrl,
            username,
            password,
        )
        return RealSmartLightDatabase(database)
    }
}