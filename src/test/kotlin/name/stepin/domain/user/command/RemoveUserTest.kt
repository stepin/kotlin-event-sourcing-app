package name.stepin.domain.user.command

import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import name.stepin.db.repository.UserRepository
import name.stepin.domain.user.event.UserRemoved
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
class RemoveUserTest {
    private lateinit var command: RemoveUser

    @MockK
    lateinit var store: EventStorePublisher

    @MockK
    lateinit var userRepository: UserRepository

    @BeforeEach
    fun setUp() {
        command = RemoveUser(store, userRepository)
    }

    @AfterEach
    fun tearDown() {
        confirmVerified(store, userRepository)
    }

    @Test
    fun `not found case`() =
        runBlocking {
            val userGuid = UUID.randomUUID()
            coEvery { userRepository.findByGuid(userGuid) } returns null

            val actual = command.execute(userGuid)

            assertEquals(ErrorCode.USER_NOT_FOUND, actual)
            coVerify(exactly = 1) { userRepository.findByGuid(userGuid) }
        }

    @Test
    fun `main case`() =
        runBlocking {
            val createdAt = LocalDateTime.of(2023, 1, 1, 1, 1)
            val eventGuid = UUID.randomUUID()
            val userGuid = UUID.randomUUID()
            val accountGuid = UUID.randomUUID()
            val userEntity =
                UserEntityFactory.userEntity(1).apply {
                    this.accountGuid = accountGuid
                    this.guid = userGuid
                }
            val event = UserRemoved(aggregatorGuid = userGuid, accountGuid = accountGuid, guid = eventGuid)
            val meta = EventMetadata(createdAt = createdAt)

            mockkStatic(UUID::class, LocalDateTime::class) {
                every { UUID.randomUUID() } returns eventGuid
                every { LocalDateTime.now() } returns createdAt
                coEvery { userRepository.findByGuid(userGuid) } returns userEntity
                coEvery { store.publish(event, meta, false) } returns userGuid

                val actual = command.execute(userGuid)

                assertNull(actual)
                coVerify(exactly = 1) { userRepository.findByGuid(userGuid) }
                coVerify(exactly = 1) { store.publish(event, meta, false) }
                verify(exactly = 1) { UUID.randomUUID() }
                verify(exactly = 1) { LocalDateTime.now() }
            }
        }
}
