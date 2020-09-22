import com.google.common.truth.Truth.assertThat
import com.squaredcandy.europa.model.SmartLight
import com.squaredcandy.europa.model.SmartLightCapability
import com.squaredcandy.europa.model.SmartLightData
import com.squaredcandy.db.smartlight.SmartLightDatabaseInterface
import com.squaredcandy.db.smartlight.model.DatabaseProvider
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
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
                password = null,)
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
        assertThat(smartLights).isNotEmpty()
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
        assertThat(inserted2).isTrue()

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

    private fun getTestSmartLightData(): SmartLightData {
        return SmartLightData(
            ipAddress = "192.168.1.${Random.nextInt(0, 255)}",
            isOn = Random.nextBoolean(),
            capabilities = listOf(
                SmartLightCapability.SmartLightColor.SmartLightKelvin(
                    kelvin = Random.nextInt(1000, 7500),
                    brightness = Random.nextFloat()
                ),
                SmartLightCapability.SmartLightLocation(
                    "Home/Bedroom"
                )
            )
        )
    }
}