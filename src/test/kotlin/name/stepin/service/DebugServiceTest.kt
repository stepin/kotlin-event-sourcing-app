package name.stepin.service

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import name.stepin.db.repository.AccountRepository
import name.stepin.db.repository.UserRepository
import name.stepin.domain.user.event.UserEvent
import name.stepin.es.store.EventStoreReader
import name.stepin.fixture.AccountEntityFactory.accountEntity
import name.stepin.fixture.EventsFactory.flow3events
import name.stepin.fixture.UserEntityFactory.userEntity
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

@ExtendWith(MockKExtension::class)
class DebugServiceTest {
    private lateinit var service: DebugService

    @MockK
    lateinit var userRepository: UserRepository

    @MockK
    lateinit var accountRepository: AccountRepository

    @MockK
    lateinit var eventStoreReader: EventStoreReader

    @BeforeEach
    fun setUp() {
        service = DebugService(userRepository, accountRepository, eventStoreReader)
    }

    @AfterEach
    fun tearDown() {
        confirmVerified(userRepository, accountRepository, eventStoreReader)
    }

    @Test
    fun `getUsers main case`() = runBlocking {
        every { userRepository.findAll() } returns flowOf(
            userEntity(1),
            userEntity(2),
            userEntity(3),
        )

        val actual = service.getUsers()

        assertEquals(listOf(1L, 2, 3), actual.map { it.id })
        verify { userRepository.findAll() }
    }

    @Test
    fun `getUsersSince main case`() = runBlocking {
        val createdAtRequest = Instant.ofEpochSecond(1000000L)
        val createdAt1 = Instant.ofEpochSecond(2000000L)
        val createdAt2 = Instant.ofEpochSecond(3000000L)
        val createdAt3 = Instant.ofEpochSecond(4000000L)
        every { userRepository.findAllByCreatedAtAfter(createdAtRequest) } returns flowOf(
            userEntity(1).apply { createdAt = createdAt1 },
            userEntity(2).apply { createdAt = createdAt2 },
            userEntity(3).apply { createdAt = createdAt3 },
        )

        val actual = service.getUsersSince(LocalDateTime.ofInstant(createdAtRequest, ZoneOffset.UTC))

        assertEquals(listOf(1L, 2, 3), actual.map { it.id })
        verify { userRepository.findAllByCreatedAtAfter(createdAtRequest) }
    }

    @Test
    fun `getUserAudit main case`() = runBlocking {
        val accountGuid = UUID.randomUUID()
        val userGuid = UUID.randomUUID()
        val eventGuid1 = UUID.randomUUID()
        val eventGuid2 = UUID.randomUUID()
        every {
            eventStoreReader.findEvents<UserEvent>("user", userGuid, maxBatchSize = 100)
        } returns flow3events(
            accountGuid = accountGuid,
            userGuid = userGuid,
            eventGuid1 = eventGuid1,
            eventGuid2 = eventGuid2,
        )
        val expected = listOf(
            "user registered with id 1 2023-01-01T01:01 UserRegistered(email=cara.rivas@example.com, " +
                "firstName=null, secondName=null, displayName=Peggy Fuller, " +
                "accountGuid=$accountGuid, aggregatorGuid=$userGuid, guid=$eventGuid1)",
            "updated UserMetaUpdated(firstName=firstName2, secondName=null, displayName=null, " +
                "aggregatorGuid=$userGuid, accountGuid=$accountGuid, guid=$eventGuid2)",
            "user deleted at 2023-01-03T01:01",
        )

        val actual = service.getUserAudit(userGuid)

        val actualList = actual.toList()
        assertEquals(expected, actualList)
        verify { eventStoreReader.findEvents<UserEvent>("user", userGuid, maxBatchSize = 100) }
    }

    @Test
    fun `getAccounts main case`() = runBlocking {
        every { accountRepository.findAll() } returns flowOf(
            accountEntity(1),
            accountEntity(2),
            accountEntity(3),
        )

        val actual = service.getAccounts()

        assertEquals(listOf(1L, 2, 3), actual.map { it.id })
        verify { accountRepository.findAll() }
    }
}
