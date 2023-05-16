package name.stepin.domain.user.projector

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import name.stepin.db.repository.AccountRepository
import name.stepin.db.repository.UserRepository
import name.stepin.domain.user.event.UserMetaUpdated
import name.stepin.domain.user.event.UserRegistered
import name.stepin.domain.user.event.UserRemoved
import name.stepin.es.store.EventMetadata
import name.stepin.exception.DomainException
import name.stepin.fixture.AccountEntityFactory.accountEntity
import name.stepin.fixture.UserEntityFactory.userEntity
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

@ExtendWith(MockKExtension::class)
class UserProjectorTest {

    private lateinit var projector: UserProjector

    @MockK
    lateinit var userRepository: UserRepository

    @MockK
    lateinit var accountRepository: AccountRepository

    @BeforeEach
    fun setUp() {
        projector = UserProjector(userRepository, accountRepository)
    }

    @AfterEach
    fun tearDown() {
        confirmVerified(userRepository, accountRepository)
    }

    @Test
    fun `handleUserMetaUpdated not found case`() = runBlocking {
        val userGuid = UUID.randomUUID()
        val event = UserMetaUpdated(
            aggregatorGuid = userGuid,
            accountGuid = UUID.randomUUID(),
            firstName = "firstName2",
            secondName = "secondName2",
            displayName = "displayName2",
        )
        coEvery { userRepository.findByGuid(userGuid) } returns null

        val exception = assertThrows<DomainException> {
            projector.handleUserMetaUpdated(event)
        }

        assertEquals("USER_NOT_FOUND", exception.message)
        coVerify(exactly = 1) { userRepository.findByGuid(userGuid) }
    }

    @Test
    fun `handleUserMetaUpdated nothing to update case`() = runBlocking {
        val userGuid = UUID.randomUUID()
        val event = UserMetaUpdated(
            aggregatorGuid = userGuid,
            accountGuid = UUID.randomUUID(),
            firstName = null,
            secondName = null,
            displayName = null,
        )
        val userEntity = userEntity(1)
        coEvery { userRepository.findByGuid(userGuid) } returns userEntity
        coEvery { userRepository.save(userEntity) } answers { firstArg() }

        projector.handleUserMetaUpdated(event)

        coVerify(exactly = 1) { userRepository.findByGuid(userGuid) }
        coVerify(exactly = 1) { userRepository.save(any()) }
    }

    @Test
    fun `handleUserMetaUpdated main case`() = runBlocking {
        val userGuid = UUID.randomUUID()
        val event = UserMetaUpdated(
            aggregatorGuid = userGuid,
            accountGuid = UUID.randomUUID(),
            firstName = "firstName2",
            secondName = "secondName2",
            displayName = "displayName2",
        )
        val userEntity = userEntity(1)
        coEvery { userRepository.findByGuid(userGuid) } returns userEntity
        coEvery { userRepository.save(userEntity) } answers { firstArg() }

        projector.handleUserMetaUpdated(event)

        coVerify(exactly = 1) { userRepository.findByGuid(userGuid) }
        coVerify(exactly = 1) { userRepository.save(any()) }
    }

    @Test
    fun `handleUserRegistered no account case`() = runBlocking {
        val userGuid = UUID.randomUUID()
        val createdAt = LocalDateTime.of(2023, 1, 1, 1, 1)
        val event = UserRegistered(
            aggregatorGuid = userGuid,
            accountGuid = UUID.randomUUID(),
            email = "dorthy.sparks@example.com",
            firstName = "firstName1",
            secondName = "secondName1",
            displayName = "Tammi Tillman",
        )
        val meta = EventMetadata(createdAt = createdAt)
        val entity = userEntity(1).apply {
            id = null
            guid = userGuid
            accountGuid = event.accountGuid
            accountId = 0
            email = "dorthy.sparks@example.com"
            firstName = "firstName1"
            secondName = "secondName1"
            displayName = "Tammi Tillman"
            this.createdAt = createdAt.toInstant(ZoneOffset.UTC)
        }
        coEvery { accountRepository.findByGuid(event.accountGuid) } returns null
        coEvery { userRepository.save(entity) } answers { firstArg() }

        projector.handleUserRegistered(event, meta)

        coVerify(exactly = 1) { userRepository.save(entity) }
        coVerify(exactly = 1) { accountRepository.findByGuid(event.accountGuid) }
    }

    @Test
    fun `handleUserRegistered main case`() = runBlocking {
        val createdAt = LocalDateTime.of(2023, 1, 1, 1, 1)
        val userGuid = UUID.randomUUID()
        val event = UserRegistered(
            aggregatorGuid = userGuid,
            accountGuid = UUID.randomUUID(),
            email = "dorthy.sparks@example.com",
            firstName = "firstName1",
            secondName = "secondName1",
            displayName = "Tammi Tillman",
        )
        val meta = EventMetadata(createdAt = createdAt)
        val accountEntity = accountEntity(1)
        val entity = userEntity(1).apply {
            id = null
            guid = userGuid
            accountGuid = event.accountGuid
            accountId = accountEntity.id!!
            email = "dorthy.sparks@example.com"
            firstName = "firstName1"
            secondName = "secondName1"
            displayName = "Tammi Tillman"
            this.createdAt = createdAt.toInstant(ZoneOffset.UTC)
        }
        coEvery { accountRepository.findByGuid(event.accountGuid) } returns accountEntity
        coEvery { userRepository.save(entity) } answers { firstArg() }

        projector.handleUserRegistered(event, meta)

        coVerify(exactly = 1) { userRepository.save(entity) }
        coVerify(exactly = 1) { accountRepository.findByGuid(event.accountGuid) }
    }

    @Test
    fun `handleUserRemoved not found case`() = runBlocking {
        val userGuid = UUID.randomUUID()
        val event = UserRemoved(aggregatorGuid = userGuid, accountGuid = UUID.randomUUID())
        val meta = EventMetadata()
        coEvery { userRepository.findByGuid(userGuid) } returns null

        val exception = assertThrows<DomainException> {
            projector.handleUserRemoved(event, meta)
        }

        assertEquals("USER_NOT_FOUND", exception.message)
        coVerify(exactly = 1) { userRepository.findByGuid(userGuid) }
    }

    @Test
    fun `handleUserRemoved main case`() = runBlocking {
        val userGuid = UUID.randomUUID()
        val event = UserRemoved(aggregatorGuid = userGuid, accountGuid = UUID.randomUUID())
        val meta = EventMetadata()
        val userEntity = userEntity(1)
        coEvery { userRepository.findByGuid(userGuid) } returns userEntity
        coEvery { userRepository.delete(userEntity) } returns Unit

        projector.handleUserRemoved(event, meta)

        coVerify(exactly = 1) { userRepository.findByGuid(userGuid) }
        coVerify(exactly = 1) { userRepository.delete(userEntity) }
    }
}
