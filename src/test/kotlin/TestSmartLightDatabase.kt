import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.squaredcandy.db.ChangeType
import com.squaredcandy.europa.model.SmartLight
import com.squaredcandy.europa.model.SmartLightCapability
import com.squaredcandy.europa.model.SmartLightData
import com.squaredcandy.db.smartlight.SmartLightDatabaseInterface
import com.squaredcandy.db.smartlight.model.DatabaseProvider
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import kotlin.random.Random
import kotlin.time.ExperimentalTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestSmartLightDatabase {

    @AfterEach
    fun setupDatabase() = runBlocking {
        database.getAllSmartLights().forEach {
            database.removeSmartLight(it.macAddress)
        }
    }

    companion object {
        private val database: SmartLightDatabaseInterface = getDatabase()

        @JvmStatic
        @AfterAll
        private fun tearDownDatabase() {
            database.closeDatabase()
        }

        private fun getDatabase(): SmartLightDatabaseInterface {
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
        assertThat(smartLights).isEmpty()

        // Insert first smart light
        val testSmartLight = getTestSmartLight()
        val inserted = database.upsertSmartLight(testSmartLight)
        assertThat(inserted).isTrue()

        // Assert added correctly
        smartLights = database.getAllSmartLights()
        assertThat(smartLights).hasSize(1)
        val smartLight = smartLights[0]
        assertThat(smartLight).isEqualTo(testSmartLight)
    }

    @Test
    fun `Insert two duplicate smart lights`() = runBlocking {
        // Check we don't have any data
        var smartLights = database.getAllSmartLights()
        assertThat(smartLights).isEmpty()

        // Insert first smart light
        val testSmartLight = getTestSmartLight()
        val inserted1 = database.upsertSmartLight(testSmartLight)
        assertThat(inserted1).isTrue()

        // Assert added correctly
        smartLights = database.getAllSmartLights()
        assertThat(smartLights).hasSize(1)
        val smartLight1 = smartLights.last()
        assertThat(smartLight1).isEqualTo(testSmartLight)

        // Insert second smart light
        val inserted2 = database.upsertSmartLight(testSmartLight)
        assertThat(inserted2).isFalse()

        // Assert added correctly
        smartLights = database.getAllSmartLights()
        assertThat(smartLights).hasSize(1)
        val smartLight2 = smartLights.last()
        assertThat(smartLight2).isEqualTo(testSmartLight)
    }

    @Test
    fun `Insert one smart light then update with second dataset`() = runBlocking {
        // Check we don't have any data
        var smartLights = database.getAllSmartLights()
        assertThat(smartLights).isEmpty()

        // Add first smart light
        val testSmartLight = getTestSmartLight()
        val inserted1 = database.upsertSmartLight(testSmartLight)
        assertThat(inserted1).isTrue()

        // Assert added correctly
        smartLights = database.getAllSmartLights()
        assertThat(smartLights).hasSize(1)
        val smartLight1 = smartLights.last()
        assertThat(smartLight1).isEqualTo(testSmartLight)

        // Add same smart light with second set of data
        val testSmartLight2 = testSmartLight.copy(
            smartLightData = testSmartLight.smartLightData.toMutableList().apply {
                add(getTestSmartLightData())
            }
        )
        val inserted2 = database.upsertSmartLight(testSmartLight2)
        assertThat(inserted2).isTrue()

        // Assert added correctly
        smartLights = database.getAllSmartLights()
        assertThat(smartLights).hasSize(1)
        val smartLight2 = smartLights.last()
        assertThat(smartLight2).isNotEqualTo(testSmartLight)
        assertThat(smartLight2.smartLightData).hasSize(2)
        assertThat(smartLight2.smartLightData).contains(testSmartLight.smartLightData.first())
        assertThat(smartLight2.smartLightData).contains(testSmartLight2.smartLightData.first())
    }

    @Test
    fun `Insert two different smart lights`() = runBlocking {
        // Check we don't have any data
        var smartLights = database.getAllSmartLights()
        assertThat(smartLights).isEmpty()

        // Add first smart light
        val testSmartLight = getTestSmartLight()
        val inserted1 = database.upsertSmartLight(testSmartLight)
        assertThat(inserted1).isTrue()

        // Assert added correctly
        smartLights = database.getAllSmartLights()
        assertThat(smartLights).hasSize(1)
        val smartLight1 = smartLights.last()
        assertThat(smartLight1).isEqualTo(testSmartLight)

        // Add second smart light
        val testSmartLight2 = getTestSmartLight2()
        val inserted2 = database.upsertSmartLight(testSmartLight2)
        assertThat(inserted2).isTrue()

        // Assert added correctly
        smartLights = database.getAllSmartLights()
        assertThat(smartLights).hasSize(2)
        val smartLight2 = smartLights.last()
        assertThat(smartLight2).isEqualTo(testSmartLight2)
    }

    @Test
    fun `Insert one smart light and remove`() = runBlocking {
        // Check we don't have any data
        var smartLights = database.getAllSmartLights()
        assertThat(smartLights).isEmpty()

        // Insert first smart light
        val testSmartLight = getTestSmartLight()
        val inserted = database.upsertSmartLight(testSmartLight)
        assertThat(inserted).isTrue()

        // Assert added correctly
        smartLights = database.getAllSmartLights()
        assertThat(smartLights).hasSize(1)
        val smartLight = smartLights[0]
        assertThat(smartLight).isEqualTo(testSmartLight)

        // Remove smart light
        val removed = database.removeSmartLight(testSmartLight.macAddress)
        assertThat(removed).isTrue()

        // Assert removed correctly
        smartLights = database.getAllSmartLights()
        assertThat(smartLights).isEmpty()
    }

    @Test
    fun `Insert one smart light and remove it twice`() = runBlocking {
        // Check we don't have any data
        var smartLights = database.getAllSmartLights()
        assertThat(smartLights).isEmpty()

        // Insert first smart light
        val testSmartLight = getTestSmartLight()
        val inserted = database.upsertSmartLight(testSmartLight)
        assertThat(inserted).isTrue()

        // Assert added correctly
        smartLights = database.getAllSmartLights()
        assertThat(smartLights).hasSize(1)
        val smartLight = smartLights[0]
        assertThat(smartLight).isEqualTo(testSmartLight)

        // Remove smart light
        var removed = database.removeSmartLight(testSmartLight.macAddress)
        assertThat(removed).isTrue()

        // Assert removed correctly
        smartLights = database.getAllSmartLights()
        assertThat(smartLights).isEmpty()

        // Remove smart light again
        removed = database.removeSmartLight(testSmartLight.macAddress)
        assertThat(removed).isFalse()

        // Assert that database is empty
        smartLights = database.getAllSmartLights()
        assertThat(smartLights).isEmpty()
    }

    @Test
    fun `Insert smart light and get it`() = runBlocking {
        // Check we don't have any data
        var smartLights = database.getAllSmartLights()
        assertThat(smartLights).isEmpty()

        // Insert first smart light
        val testSmartLight = getTestSmartLight()
        val inserted = database.upsertSmartLight(testSmartLight)
        assertThat(inserted).isTrue()

        // Assert added correctly
        smartLights = database.getAllSmartLights()
        assertThat(smartLights).hasSize(1)
        val smartLight = smartLights[0]
        assertThat(smartLight).isEqualTo(testSmartLight)

        // Get smart light
        val smartLight2 = database.getSmartLight(testSmartLight.macAddress)
        assertThat(smartLight2).isNotNull()
        assertThat(smartLight2).isEqualTo(testSmartLight)
    }

    @Test
    fun `Insert smart light and get the wrong one`() = runBlocking {
        // Check we don't have any data
        var smartLights = database.getAllSmartLights()
        assertThat(smartLights).isEmpty()

        // Insert first smart light
        val testSmartLight = getTestSmartLight()
        val inserted = database.upsertSmartLight(testSmartLight)
        assertThat(inserted).isTrue()

        // Assert added correctly
        smartLights = database.getAllSmartLights()
        assertThat(smartLights).hasSize(1)
        val smartLight = smartLights[0]
        assertThat(smartLight).isEqualTo(testSmartLight)

        // Get wrong smart light
        val smartLight2 = database.getSmartLight(testSmartLight.macAddress + "1")
        assertThat(smartLight2).isNull()
    }

    @ExperimentalTime
    @Test
    fun `Insert smart light and receive it through the flow`() = runBlocking {
        // Check we don't have any data
        val smartLights = database.getAllSmartLights()
        assertThat(smartLights).isEmpty()

        // Setup data
        val testSmartLight = getTestSmartLight()
        // Setup flow
        database.getOnSmartLightChanged(testSmartLight.macAddress).test {
            val inserted = database.upsertSmartLight(testSmartLight)
            assertThat(inserted).isTrue()
            val item = expectItem()
            assertThat(item).isInstanceOf(ChangeType.Inserted::class.java)
            assertThat((item as ChangeType.Inserted).item).isEqualTo(testSmartLight)

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @ExperimentalTime
    @Test
    fun `Insert two smart lights and receive it through the flow`() = runBlocking {
        // Check we don't have any data
        val smartLights = database.getAllSmartLights()
        assertThat(smartLights).isEmpty()

        // Setup data
        val testSmartLight = getTestSmartLight()
        // Setup flow
        database.getOnSmartLightChanged(testSmartLight.macAddress).test {
            var inserted = database.upsertSmartLight(testSmartLight)
            assertThat(inserted).isTrue()
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
            assertThat(inserted).isTrue()
            item = expectItem()
            assertThat(item).isInstanceOf(ChangeType.Updated::class.java)
            assertThat((item as ChangeType.Updated).item).isEqualTo(testSmartLight2)

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @ExperimentalTime
    @Test
    fun `Insert two smart lights of the same data and receive only one in the flow`() = runBlocking {
        // Check we don't have any data
        val smartLights = database.getAllSmartLights()
        assertThat(smartLights).isEmpty()

        // Setup data
        val testSmartLight = getTestSmartLight()
        // Setup flow
        database.getOnSmartLightChanged(testSmartLight.macAddress).test {
            var inserted = database.upsertSmartLight(testSmartLight)
            assertThat(inserted).isTrue()
            var item = expectItem()
            assertThat(item).isInstanceOf(ChangeType.Inserted::class.java)
            assertThat((item as ChangeType.Inserted).item).isEqualTo(testSmartLight)

            inserted = database.upsertSmartLight(testSmartLight)
            assertThat(inserted).isFalse()

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @ExperimentalTime
    @Test
    fun `Insert smart light and delete it and receive only the insert data in the flow`() = runBlocking {
        // Check we don't have any data
        val smartLights = database.getAllSmartLights()
        assertThat(smartLights).isEmpty()

        // Setup data
        val testSmartLight = getTestSmartLight()
        // Setup flow
        database.getOnSmartLightChanged(testSmartLight.macAddress).test {
            val inserted = database.upsertSmartLight(testSmartLight)
            assertThat(inserted).isTrue()
            var item = expectItem()
            assertThat(item).isInstanceOf(ChangeType.Inserted::class.java)
            assertThat((item as ChangeType.Inserted).item).isEqualTo(testSmartLight)

            val removed = database.removeSmartLight(testSmartLight.macAddress)
            assertThat(removed).isTrue()
            item = expectItem()
            assertThat(item).isInstanceOf(ChangeType.Removed::class.java)

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
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
    private fun getTestSmartLightData(): SmartLightData {
        return SmartLightData(
            ipAddress = "192.168.1.${Random.nextInt(0, 255)}",
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