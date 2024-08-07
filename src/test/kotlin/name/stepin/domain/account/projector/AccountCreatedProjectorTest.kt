package name.stepin.domain.account.projector

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import name.stepin.db.entity.AccountEntity
import name.stepin.db.repository.AccountRepository
import name.stepin.db.repository.UserRepository
import name.stepin.domain.account.event.AccountCreated
import name.stepin.es.store.EventMetadata
import name.stepin.exception.DomainException
import name.stepin.fixture.UserEntityFactory.userEntity
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
class AccountCreatedProjectorTest {
    private lateinit var projector: AccountCreatedProjector

    @MockK
    lateinit var userRepository: UserRepository

    @MockK
    lateinit var accountRepository: AccountRepository

    @BeforeEach
    fun setUp() {
        projector = AccountCreatedProjector(userRepository, accountRepository)
    }

    @AfterEach
    fun tearDown() {
        confirmVerified(userRepository, accountRepository)
    }

    @Test
    fun `user not found case`() =
        runBlocking {
            val userGuid = UUID.randomUUID()
            val event =
                AccountCreated(
                    name = "Lorraine Spence",
                    userGuid = userGuid,
                    accountGuid = UUID.randomUUID(),
                )
            coEvery { userRepository.findByGuid(userGuid) } returns null

            val exception =
                assertThrows<DomainException> {
                    projector.handle(event, EventMetadata())
                }

            assertEquals("USER_NOT_FOUND", exception.message)
            coVerify(exactly = 1) { userRepository.findByGuid(userGuid) }
        }

    @Test
    fun `main case`() =
        runBlocking {
            val userGuid = UUID.randomUUID()
            val event =
                AccountCreated(
                    name = "Lorraine Spence",
                    userGuid = userGuid,
                    accountGuid = UUID.randomUUID(),
                )
            coEvery { userRepository.findByGuid(userGuid) } returns userEntity(1)
            coEvery { accountRepository.save(any()) } answers {
                val entity: AccountEntity = firstArg()
                entity.apply { id = 100 }
            }
            coEvery { userRepository.save(any()) } answers { firstArg() }

            projector.handle(event, EventMetadata())

            coVerify(exactly = 1) { userRepository.findByGuid(userGuid) }
            coVerify(exactly = 1) { accountRepository.save(any()) }
            coVerify(exactly = 1) { userRepository.save(any()) }
        }
}
