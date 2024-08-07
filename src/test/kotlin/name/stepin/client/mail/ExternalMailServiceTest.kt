package name.stepin.client.mail

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ExternalMailServiceTest {
    private lateinit var service: ExternalMailService

    @BeforeEach
    fun setUp() {
        service = ExternalMailService()
    }

    @Test
    fun `sendEmail main case`() =
        runBlocking {
            // dummy test for dummy method
            service.sendEmail(
                displayName = "Roosevelt Thompson",
                email = "muriel.stephenson@example.com",
                subject = "torquent",
                text = "nonumes",
            )
        }
}
