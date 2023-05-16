package name.stepin.domain.user.email

import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import name.stepin.client.mail.ExternalMailService
import name.stepin.config.AppConfig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SendEmailServiceTest {
    private lateinit var service: SendEmailService

    @MockK
    lateinit var externalMailService: ExternalMailService

    @MockK
    lateinit var appConfig: AppConfig

    @BeforeEach
    fun setUp() {
        service = SendEmailService(externalMailService, appConfig)
    }

    @AfterEach
    fun tearDown() {
        confirmVerified(externalMailService, appConfig)
    }

    @Test
    fun `sendEmailConfirmationEmail blank displayName case`() = runBlocking {
        val emailText = """
            Здравствуйте, Пользователь!

            Спасибо за регистрацию . Пожалуйста, перейдите по ссылке:

            https://example.com/baseUrl/welcome
        """.trimIndent()
        every { appConfig.baseUrl } returns "https://example.com/baseUrl"
        coEvery {
            externalMailService.sendEmail(
                displayName = "Пользователь",
                email = "email1@example.com",
                subject = "Спасибо за регистрацию!",
                text = emailText,
            )
        } returns Unit

        service.sendEmailConfirmationEmail("", "email1@example.com", "token1")

        verify(exactly = 1) { appConfig.baseUrl }
        coVerify(exactly = 1) {
            externalMailService.sendEmail(
                displayName = "Пользователь",
                email = "email1@example.com",
                subject = "Спасибо за регистрацию!",
                text = any(),
            )
        }
    }

    @Test
    fun `sendEmailConfirmationEmail main case`() = runBlocking {
        val emailText = """
            Здравствуйте, name1!

            Спасибо за регистрацию . Пожалуйста, перейдите по ссылке:

            https://example.com/baseUrl/welcome
        """.trimIndent()
        every { appConfig.baseUrl } returns "https://example.com/baseUrl"
        coEvery {
            externalMailService.sendEmail(
                displayName = "name1",
                email = "email1@example.com",
                subject = "Спасибо за регистрацию!",
                text = emailText,
            )
        } returns Unit

        service.sendEmailConfirmationEmail("name1", "email1@example.com", "token1")

        verify(exactly = 1) { appConfig.baseUrl }
        coVerify(exactly = 1) {
            externalMailService.sendEmail(
                displayName = "name1",
                email = "email1@example.com",
                subject = "Спасибо за регистрацию!",
                text = any(),
            )
        }
    }
}
