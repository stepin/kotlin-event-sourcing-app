package name.stepin.domain.user.command

import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import name.stepin.db.repository.UserRepository
import name.stepin.domain.account.event.AccountCreated
import name.stepin.domain.user.command.RegisterUser.Response
import name.stepin.domain.user.event.UserRegistered
import name.stepin.es.store.EventMetadata
import name.stepin.es.store.EventStorePublisher
import name.stepin.exception.ErrorCode
import name.stepin.fixture.UserEntityFactory
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockKExtension::class)
class RegisterUserTest {
    private lateinit var command: RegisterUser

    @MockK
    lateinit var store: EventStorePublisher

    @MockK
    lateinit var userRepository: UserRepository

    @BeforeEach
    fun setUp() {
        command = RegisterUser(store, userRepository)
    }

    @AfterEach
    fun tearDown() {
        confirmVerified(store, userRepository)
    }

    @Test
    fun `already registered case`() = runBlocking {
        val userEntity = UserEntityFactory.userEntity(1)
        val params = RegisterUser.Params(
            email = "corey.kent@example.com",
            firstName = "firstName1",
            secondName = "secondName1",
            displayName = "displayName1",
        )
        coEvery { userRepository.findByEmail("corey.kent@example.com") } returns userEntity

        val actual = command.execute(params)

        assertEquals(Response.Error(ErrorCode.USER_ALREADY_REGISTERED), actual)
        coVerify(exactly = 1) { userRepository.findByEmail(any()) }
    }

    @Test
    fun `handleUserRemoved main case`() = runBlocking {
        val createdAt = LocalDateTime.of(2023, 1, 1, 1, 1)
        val eventGuid = UUID.randomUUID()
        val eventGuid2 = UUID.randomUUID()
        val userGuid = UUID.randomUUID()
        val accountGuid = UUID.randomUUID()
        val event = UserRegistered(
            email = "corey.kent@example.com",
            firstName = "firstName1",
            secondName = "secondName1",
            displayName = "displayName1",
            accountGuid = accountGuid,
            aggregatorGuid = userGuid,
            guid = eventGuid,
        )
        val event2 = AccountCreated(
            name = "Неизвестная компания",
            accountGuid = accountGuid,
            userGuid = userGuid,
            guid = eventGuid2,
        )
        val meta = EventMetadata(createdAt = createdAt)
        val params = RegisterUser.Params(
            email = "corey.kent@example.com",
            firstName = "firstName1",
            secondName = "secondName1",
            displayName = "displayName1",
        )

        mockkStatic(UUID::class, LocalDateTime::class) {
            every { UUID.randomUUID() } returnsMany listOf(accountGuid, userGuid, eventGuid, eventGuid2)
            every { LocalDateTime.now() } returns createdAt
            coEvery { userRepository.findByEmail("corey.kent@example.com") } returns null
            coEvery { store.publish(event, meta, false) } returns userGuid
            coEvery { store.publish(event2, meta, false) } returns accountGuid

            val actual = command.execute(params)

            assertEquals(Response.Created(userGuid), actual)
            coVerify(exactly = 1) { userRepository.findByEmail("corey.kent@example.com") }
            coVerify(exactly = 2) { store.publish(any(), any(), false) }
            verify(exactly = 4) { UUID.randomUUID() }
            verify(exactly = 2) { LocalDateTime.now() }
        }
    }

    @Test
    fun `handleUserRemoved main case with calc display name`() = runBlocking {
        val createdAt = LocalDateTime.of(2023, 1, 1, 1, 1)
        val eventGuid = UUID.randomUUID()
        val eventGuid2 = UUID.randomUUID()
        val userGuid = UUID.randomUUID()
        val accountGuid = UUID.randomUUID()
        val event = UserRegistered(
            email = "corey.kent@example.com",
            firstName = "firstName1",
            secondName = "secondName1",
            displayName = "firstName1 secondName1",
            accountGuid = accountGuid,
            aggregatorGuid = userGuid,
            guid = eventGuid,
        )
        val event2 = AccountCreated(
            name = "Неизвестная компания",
            accountGuid = accountGuid,
            userGuid = userGuid,
            guid = eventGuid2,
        )
        val meta = EventMetadata(createdAt = createdAt)
        val params = RegisterUser.Params(
            email = "corey.kent@example.com",
            firstName = "firstName1",
            secondName = "secondName1",
            displayName = null,
        )

        mockkStatic(UUID::class, LocalDateTime::class) {
            every { UUID.randomUUID() } returnsMany listOf(accountGuid, userGuid, eventGuid, eventGuid2)
            every { LocalDateTime.now() } returns createdAt
            coEvery { userRepository.findByEmail("corey.kent@example.com") } returns null
            coEvery { store.publish(event, meta, false) } returns userGuid
            coEvery { store.publish(event2, meta, false) } returns accountGuid

            val actual = command.execute(params)

            assertEquals(Response.Created(userGuid), actual)
            coVerify(exactly = 1) { userRepository.findByEmail("corey.kent@example.com") }
            coVerify(exactly = 2) { store.publish(any(), any(), false) }
            verify(exactly = 4) { UUID.randomUUID() }
            verify(exactly = 2) { LocalDateTime.now() }
        }
    }
}
