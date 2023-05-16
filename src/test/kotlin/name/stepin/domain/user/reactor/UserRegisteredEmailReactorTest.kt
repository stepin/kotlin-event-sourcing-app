package name.stepin.domain.user.reactor

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import name.stepin.domain.user.email.SendEmailService
import name.stepin.domain.user.event.UserRegistered
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
class UserRegisteredEmailReactorTest {

    private lateinit var reactor: UserRegisteredEmailReactor

    @MockK
    lateinit var emailService: SendEmailService

    @BeforeEach
    fun setUp() {
        reactor = UserRegisteredEmailReactor(emailService)
    }

    @AfterEach
    fun tearDown() {
        confirmVerified(emailService)
    }

    @Test
    fun `main case`() = runBlocking {
        val userGuid = UUID.randomUUID()
        val token = userGuid.toString()
        val e = UserRegistered(
            email = "ernie.mcdowell@example.com",
            firstName = "firstName1",
            secondName = "secondName1",
            displayName = "Stephan Hendricks",
            accountGuid = UUID.randomUUID(),
            aggregatorGuid = userGuid,
        )
        coEvery {
            emailService.sendEmailConfirmationEmail(
                name = "Stephan Hendricks",
                email = "ernie.mcdowell@example.com",
                token = token,
            )
        } returns Unit

        reactor.handle(e)

        coVerify(exactly = 1) {
            emailService.sendEmailConfirmationEmail(
                name = "Stephan Hendricks",
                email = "ernie.mcdowell@example.com",
                token = token,
            )
        }
    }
}
