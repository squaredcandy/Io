import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.squaredcandy.io.db.util.ChangeType
import com.squaredcandy.europa.model.SmartLight
import com.squaredcandy.europa.model.SmartLightCapability
import com.squaredcandy.europa.model.SmartLightData
import com.squaredcandy.europa.util.Result
import com.squaredcandy.io.db.smartlight.SmartLightDatabase
import com.squaredcandy.io.db.util.DatabaseProvider
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import kotlin.random.Random
import kotlin.time.ExperimentalTime
import ResultSubject.Companion.assertThat
import ResultListSubject.Companion.assertThat
import com.squaredcandy.europa.util.getValueOrNull
import com.squaredcandy.europa.util.getValueOrThrow
import com.squaredcandy.europa.util.onSuccessSuspended
import com.squaredcandy.io.db.util.DatabaseErrorType
import com.squaredcandy.io.db.util.DatabaseException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.OffsetDateTime

@ExperimentalCoroutinesApi
@ExperimentalTime
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestSmartLightDatabase {

    @AfterEach
    fun setupDatabase(): Unit = runBlocking {
        database.getAllSmartLights().onSuccessSuspended { smartLights ->
            smartLights.forEach { 
                database.removeSmartLight(it.macAddress) 
            }
        }
    }

    companion object {
        private val database: SmartLightDatabase = getDatabase()

        @JvmStatic
        @AfterAll
        private fun tearDownDatabase() {
            database.closeDatabase()
        }

        private fun getDatabase(): SmartLightDatabase {
            return DatabaseProvider.getSmartLightDatabase(
                driverClassName = "org.h2.Driver",
                jdbcUrl = "jdbc:h2:mem:test",
                username = null,
                password = null,
            )
        }
    }

    @Test
    fun `Insert one smart light`() = runBlocking {
        // Check we don't have any data
        var smartLights = database.getAllSmartLights()
        assertThat(smartLights).isSuccessListEmpty()

        // Insert first smart light
        val testSmartLight = getTestSmartLight()
        val inserted = database.upsertSmartLight(testSmartLight)
        assertThat(inserted).isSuccess()

        // Assert added correctly
        smartLights = database.getAllSmartLights()
        assertThat(smartLights).isSuccessListHasSize(1)
        val smartLight = smartLights.getItem(0)
        assertThat(smartLight).isEqualTo(testSmartLight)
    }

    @Test
    fun `Insert two duplicate smart lights`() = runBlocking {
        // Check we don't have any data
        var smartLights = database.getAllSmartLights()
        assertThat(smartLights).isSuccessListEmpty()

        // Insert first smart light
        val testSmartLight = getTestSmartLight()
        val inserted1 = database.upsertSmartLight(testSmartLight)
        assertThat(inserted1).isSuccess()

        // Assert added correctly
        smartLights = database.getAllSmartLights()
        assertThat(smartLights).isSuccessListHasSize(1)
        val smartLight1 = smartLights.getLastItem()
        assertThat(smartLight1).isEqualTo(testSmartLight)

        // Insert second smart light
        val inserted2 = database.upsertSmartLight(testSmartLight)
        assertThat(inserted2).isFailure()
        assertThat(inserted2).isFailureEqualTo(DatabaseException(DatabaseErrorType.NO_CHANGE))

        // Assert added correctly
        smartLights = database.getAllSmartLights()
        assertThat(smartLights).isSuccessListHasSize(1)
        val smartLight2 = smartLights.getLastItem()
        assertThat(smartLight2).isEqualTo(testSmartLight)
    }

    @Test
    fun `Insert one smart light then update with second dataset`() = runBlocking {
        // Check we don't have any data
        var smartLights = database.getAllSmartLights()
        assertThat(smartLights).isSuccessListEmpty()

        // Add first smart light
        val testSmartLight = getTestSmartLight()
        val inserted1 = database.upsertSmartLight(testSmartLight)
        assertThat(inserted1).isSuccess()

        // Assert added correctly
        smartLights = database.getAllSmartLights()
        assertThat(smartLights).isSuccessListHasSize(1)
        val smartLight1 = smartLights.getLastItem()
        assertThat(smartLight1).isEqualTo(testSmartLight)

        // Add same smart light with second set of data
        val testSmartLight2 = testSmartLight.copy(
            smartLightData = testSmartLight.smartLightData.toMutableList().apply {
                add(getTestSmartLightData())
            }
        )
        val inserted2 = database.upsertSmartLight(testSmartLight2)
        assertThat(inserted2).isSuccess()

        // Assert added correctly
        smartLights = database.getAllSmartLights()
        assertThat(smartLights).isSuccessListHasSize(1)
        val smartLight2 = smartLights.getLastItem()
        assertThat(smartLight2).isNotEqualTo(testSmartLight)
        assertThat(smartLight2.smartLightData).hasSize(2)
        assertThat(smartLight2.smartLightData).contains(testSmartLight.smartLightData.first())
        assertThat(smartLight2.smartLightData).contains(testSmartLight2.smartLightData.first())
    }

    @Test
    fun `Insert one smart light then update with second dataset with same timestamp`() = runBlocking {
        // Check we don't have any data
        var smartLights = database.getAllSmartLights()
        assertThat(smartLights).isSuccessListEmpty()

        // Add first smart light
        val testSmartLight = getTestSmartLight()
        val inserted1 = database.upsertSmartLight(testSmartLight)
        assertThat(inserted1).isSuccess()

        // Assert added correctly
        smartLights = database.getAllSmartLights()
        assertThat(smartLights).isSuccessListHasSize(1)
        val smartLight1 = smartLights.getLastItem()
        assertThat(smartLight1).isEqualTo(testSmartLight)

        // Add same smart light with second set of data
        val testSmartLight2 = testSmartLight.copy(
            smartLightData = testSmartLight.smartLightData.toMutableList().apply {
                add(getTestSmartLightData(testSmartLight.smartLightData.last().timestamp))
            }
        )
        val inserted2 = database.upsertSmartLight(testSmartLight2)
        assertThat(inserted2).isFailure()

        // Assert added correctly
        smartLights = database.getAllSmartLights()
        assertThat(smartLights).isSuccessListHasSize(1)
        val smartLight2 = smartLights.getLastItem()
        assertThat(smartLight2).isEqualTo(testSmartLight)
    }


    @Test
    fun `Insert one smart light then update with the same dataset with different timestamp`() = runBlocking {
        // Check we don't have any data
        var smartLights = database.getAllSmartLights()
        assertThat(smartLights).isSuccessListEmpty()

        // Add first smart light
        val testSmartLight = getTestSmartLight()
        val inserted1 = database.upsertSmartLight(testSmartLight)
        assertThat(inserted1).isSuccess()

        // Assert added correctly
        smartLights = database.getAllSmartLights()
        assertThat(smartLights).isSuccessListHasSize(1)
        val smartLight1 = smartLights.getLastItem()
        assertThat(smartLight1).isEqualTo(testSmartLight)

        // Add same smart light with second set of data
        val testSmartLight2 = testSmartLight.copy(
            smartLightData = testSmartLight.smartLightData.toMutableList().apply {
                add(testSmartLight.smartLightData.first().copy(
                    timestamp = OffsetDateTime.now()
                ))
            }
        )
        val inserted2 = database.upsertSmartLight(testSmartLight2)
        assertThat(inserted2).isFailure()

        // Assert added correctly
        smartLights = database.getAllSmartLights()
        assertThat(smartLights).isSuccessListHasSize(1)
        val smartLight2 = smartLights.getLastItem()
        assertThat(smartLight2).isEqualTo(testSmartLight)
    }

    @Test
    fun `Insert one smart light then update with a dataset with a prior timestamp`() = runBlocking {
        // Check we don't have any data
        var smartLights = database.getAllSmartLights()
        assertThat(smartLights).isSuccessListEmpty()

        // Add first smart light
        val testSmartLight = getTestSmartLight()
        val inserted1 = database.upsertSmartLight(testSmartLight)
        assertThat(inserted1).isSuccess()

        // Assert added correctly
        smartLights = database.getAllSmartLights()
        assertThat(smartLights).isSuccessListHasSize(1)
        val smartLight1 = smartLights.getLastItem()
        assertThat(smartLight1).isEqualTo(testSmartLight)

        // Add same smart light with second set of data
        val testSmartLight2 = testSmartLight.copy(
            smartLightData = testSmartLight.smartLightData.toMutableList().apply {
                add(getTestSmartLightData(
                    customTimestamp = OffsetDateTime.now().minusMinutes(1L)
                ))
            }
        )
        val inserted2 = database.upsertSmartLight(testSmartLight2)
        assertThat(inserted2).isSuccess()

        // Assert added correctly
        smartLights = database.getAllSmartLights()
        assertThat(smartLights).isSuccessListHasSize(1)
        val smartLight2 = smartLights.getLastItem()
        assertThat(smartLight2.smartLightData).hasSize(2)
        assertThat(smartLight2.smartLightData).contains(testSmartLight.smartLightData.first())
        assertThat(smartLight2.smartLightData).contains(testSmartLight2.smartLightData.first())
    }

    @Test
    fun `Insert two different smart lights`() = runBlocking {
        // Check we don't have any data
        var smartLights = database.getAllSmartLights()
        assertThat(smartLights).isSuccessListEmpty()

        // Add first smart light
        val testSmartLight = getTestSmartLight()
        val inserted1 = database.upsertSmartLight(testSmartLight)
        assertThat(inserted1).isSuccess()

        // Assert added correctly
        smartLights = database.getAllSmartLights()
        assertThat(smartLights).isSuccessListHasSize(1)
        val smartLight1 = smartLights.getLastItem()
        assertThat(smartLight1).isEqualTo(testSmartLight)

        // Add second smart light
        val testSmartLight2 = getTestSmartLight2()
        val inserted2 = database.upsertSmartLight(testSmartLight2)
        assertThat(inserted2).isSuccess()

        // Assert added correctly
        smartLights = database.getAllSmartLights()
        assertThat(smartLights).isSuccessListHasSize(2)
        val smartLight2 = smartLights.getLastItem()
        assertThat(smartLight2).isEqualTo(testSmartLight2)
    }

    @Test
    fun `Insert one smart light and remove`() = runBlocking {
        // Check we don't have any data
        var smartLights = database.getAllSmartLights()
        assertThat(smartLights).isSuccessListEmpty()

        // Insert first smart light
        val testSmartLight = getTestSmartLight()
        val inserted = database.upsertSmartLight(testSmartLight)
        assertThat(inserted).isSuccess()

        // Assert added correctly
        smartLights = database.getAllSmartLights()
        assertThat(smartLights).isSuccessListHasSize(1)
        val smartLight = smartLights.getItem(0)
        assertThat(smartLight).isEqualTo(testSmartLight)

        // Remove smart light
        val removed = database.removeSmartLight(testSmartLight.macAddress)
        assertThat(removed).isSuccess()

        // Assert removed correctly
        smartLights = database.getAllSmartLights()
        assertThat(smartLights).isSuccessListEmpty()
    }

    @Test
    fun `Insert one smart light and remove it twice`() = runBlocking {
        // Check we don't have any data
        var smartLights = database.getAllSmartLights()
        assertThat(smartLights).isSuccessListEmpty()

        // Insert first smart light
        val testSmartLight = getTestSmartLight()
        val inserted = database.upsertSmartLight(testSmartLight)
        assertThat(inserted).isSuccess()

        // Assert added correctly
        smartLights = database.getAllSmartLights()
        assertThat(smartLights).isSuccessListHasSize(1)
        val smartLight = smartLights.getItem(0)
        assertThat(smartLight).isEqualTo(testSmartLight)

        // Remove smart light
        var removed = database.removeSmartLight(testSmartLight.macAddress)
        assertThat(removed).isSuccess()

        // Assert removed correctly
        smartLights = database.getAllSmartLights()
        assertThat(smartLights).isSuccessListEmpty()

        // Remove smart light again
        removed = database.removeSmartLight(testSmartLight.macAddress)
        assertThat(removed).isFailure()

        // Assert that database is empty
        smartLights = database.getAllSmartLights()
        assertThat(smartLights).isSuccessListEmpty()
    }

    @Test
    fun `Insert smart light and get it`() = runBlocking {
        // Check we don't have any data
        var smartLights = database.getAllSmartLights()
        assertThat(smartLights).isSuccessListEmpty()

        // Insert first smart light
        val testSmartLight = getTestSmartLight()
        val inserted = database.upsertSmartLight(testSmartLight)
        assertThat(inserted).isSuccess()

        // Assert added correctly
        smartLights = database.getAllSmartLights()
        assertThat(smartLights).isSuccessListHasSize(1)
        val smartLight = smartLights.getItem(0)
        assertThat(smartLight).isEqualTo(testSmartLight)

        // Get smart light
        val smartLight2 = database.getSmartLight(testSmartLight.macAddress)
        assertThat(smartLight2).isSuccess()
        assertThat(smartLight2).isSuccessEqualTo(testSmartLight)
    }

    @Test
    fun `Insert smart light and get the wrong one`() = runBlocking {
        // Check we don't have any data
        var smartLights = database.getAllSmartLights()
        assertThat(smartLights).isSuccessListEmpty()

        // Insert first smart light
        val testSmartLight = getTestSmartLight()
        val inserted = database.upsertSmartLight(testSmartLight)
        assertThat(inserted).isSuccess()

        // Assert added correctly
        smartLights = database.getAllSmartLights()
        assertThat(smartLights).isSuccessListHasSize(1)
        val smartLight = smartLights.getItem(0)
        assertThat(smartLight).isEqualTo(testSmartLight)

        // Get wrong smart light
        val smartLight2 = database.getSmartLight(testSmartLight.macAddress + "1")
        assertThat(smartLight2).isFailure()
        assertThat(smartLight2).isFailureEqualTo(DatabaseException(DatabaseErrorType.NOT_FOUND))
    }

    @Test
    fun `Insert smart light and receive it through the flow`() = runBlocking {
        // Check we don't have any data
        val smartLights = database.getAllSmartLights()
        assertThat(smartLights).isSuccessListEmpty()

        // Setup data
        val testSmartLight = getTestSmartLight()
        // Setup flow
        database.getOnSmartLightChanged(testSmartLight.macAddress).test {
            val inserted = database.upsertSmartLight(testSmartLight)
            assertThat(inserted).isSuccess()
            val item = expectItem()
            assertThat(item).isInstanceOf(ChangeType.Inserted::class.java)
            assertThat((item as ChangeType.Inserted).item).isEqualTo(testSmartLight)

            expectNoEvents()
        }
    }

    @Test
    fun `Insert two smart lights and receive it through the flow`() = runBlocking {
        // Check we don't have any data
        val smartLights = database.getAllSmartLights()
        assertThat(smartLights).isSuccessListEmpty()

        // Setup data
        val testSmartLight = getTestSmartLight()
        // Setup flow
        database.getOnSmartLightChanged(testSmartLight.macAddress).test {
            var inserted = database.upsertSmartLight(testSmartLight)
            assertThat(inserted).isSuccess()
            var item = expectItem()
            assertThat(item).isInstanceOf(ChangeType.Inserted::class.java)
            assertThat((item as ChangeType.Inserted).item).isEqualTo(testSmartLight)

            val testSmartLight2 = testSmartLight.copy(
                name = "new name",
                smartLightData = testSmartLight.smartLightData.toMutableList().apply {
                    add(getTestSmartLightData())
                }
            )
            inserted = database.upsertSmartLight(testSmartLight2)
            assertThat(inserted).isSuccess()
            item = expectItem()
            assertThat(item).isInstanceOf(ChangeType.Updated::class.java)
            assertThat((item as ChangeType.Updated).item).isEqualTo(testSmartLight2)

            expectNoEvents()
        }
    }

    @Test
    fun `Insert two smart lights of the same data and receive only one in the flow`() = runBlocking {
        // Check we don't have any data
        val smartLights = database.getAllSmartLights()
        assertThat(smartLights).isSuccessListEmpty()

        // Setup data
        val testSmartLight = getTestSmartLight()
        // Setup flow
        database.getOnSmartLightChanged(testSmartLight.macAddress).test {
            var inserted = database.upsertSmartLight(testSmartLight)
            assertThat(inserted).isSuccess()
            val item = expectItem()
            assertThat(item).isInstanceOf(ChangeType.Inserted::class.java)
            assertThat((item as ChangeType.Inserted).item).isEqualTo(testSmartLight)

            inserted = database.upsertSmartLight(testSmartLight)
            assertThat(inserted).isFailure()
            assertThat(inserted).isFailureEqualTo(DatabaseException(DatabaseErrorType.NO_CHANGE))

            expectNoEvents()
        }
    }

    @Test
    fun `Insert smart light and delete it and receive only the insert data in the flow`() = runBlocking {
        // Check we don't have any data
        val smartLights = database.getAllSmartLights()
        assertThat(smartLights).isSuccessListEmpty()

        // Setup data
        val testSmartLight = getTestSmartLight()
        // Setup flow
        database.getOnSmartLightChanged(testSmartLight.macAddress).test {
            val inserted = database.upsertSmartLight(testSmartLight)
            assertThat(inserted).isSuccess()
            var item = expectItem()
            assertThat(item).isInstanceOf(ChangeType.Inserted::class.java)
            assertThat((item as ChangeType.Inserted).item).isEqualTo(testSmartLight)

            val removed = database.removeSmartLight(testSmartLight.macAddress)
            assertThat(removed).isSuccess()
            item = expectItem()
            assertThat(item).isInstanceOf(ChangeType.Removed::class.java)

            expectNoEvents()
        }
    }
    
    private fun <T> Result<List<T>>.get(): List<T> {
        return this.getValueOrThrow()
    }

    private fun <T> Result<List<T>>.getItem(index: Int): T {
        return this.getValueOrThrow()[index]
    }

    private fun <T> Result<List<T>>.getLastItem(): T {
        return this.getValueOrThrow().last()
    }

    private fun getTestSmartLight(): SmartLight {
        return SmartLight(
            name = "Test Smart Light",
            macAddress = "12:34:56:78:90:10",
            smartLightData = listOf(
                getTestSmartLightData()
            )
        )
    }

    private fun getTestSmartLight2(): SmartLight {
        return SmartLight(
            name = "Test Smart Light2",
            macAddress = "11:12:13:14:15:16",
            smartLightData = listOf(
                getTestSmartLightData()
            )
        )
    }

    private var flip = false
        get() {
            return field.apply {
                field = !field
            }
        }
    private fun getTestSmartLightData(customTimestamp: OffsetDateTime = OffsetDateTime.now()): SmartLightData {
        return SmartLightData(
            ipAddress = "192.168.1.${Random.nextInt(0, 255)}",
            timestamp = customTimestamp,
            isOn = Random.nextBoolean(),
            capabilities = listOf(
                if(flip){
                    SmartLightCapability.SmartLightColor.SmartLightKelvin(
                        kelvin = Random.nextInt(1000, 7500),
                        brightness = Random.nextFloat(),
                    )
                } else {
                    SmartLightCapability.SmartLightColor.SmartLightHSB(
                        hue = Random.nextFloat(),
                        saturation = Random.nextFloat(),
                        brightness = Random.nextFloat(),
                    )
                },
                SmartLightCapability.SmartLightLocation(
                    "Home/Bedroom",
                )
            )
        )
    }
}