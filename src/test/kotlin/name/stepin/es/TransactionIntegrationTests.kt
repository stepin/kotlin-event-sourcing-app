package name.stepin.es

import com.ninjasquad.springmockk.SpykBean
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import name.stepin.db.dao.EventDao
import name.stepin.db.entity.UserEntity
import name.stepin.db.repository.UserRepository
import name.stepin.db.sql.tables.references.EVENTS
import name.stepin.domain.user.projector.UserProjector
import name.stepin.domain.user.reactor.UserRegisteredEmailReactor
import name.stepin.es.store.EventMetadata
import name.stepin.es.store.EventStorePublisher
import name.stepin.fixture.EventsFactory.userRegistered
import name.stepin.fixture.PostgresFactory.initDb
import name.stepin.fixture.PostgresFactory.postgres
import name.stepin.fixture.PostgresFactory.postgresProperties
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.lang.reflect.InvocationTargetException

@Testcontainers
@SpringBootTest
@ExtendWith(MockKExtension::class)
class TransactionIntegrationTests {
    @Autowired
    lateinit var eventStorePublisher: EventStorePublisher

    @Autowired
    lateinit var eventsDao: EventDao

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var db: DSLContext

    @SpykBean
    lateinit var userProjector: UserProjector

    @SpykBean
    lateinit var userRegisteredEmailReactor: UserRegisteredEmailReactor

    companion object {
        @Container
        var postgresContainer = postgres()

        @JvmStatic
        @DynamicPropertySource
        fun postgresContainerProperties(registry: DynamicPropertyRegistry) {
            postgresProperties(registry)
        }
    }

    @BeforeEach
    fun setUp(): Unit =
        runBlocking {
            initDb(postgresContainer)
            db.delete(EVENTS).awaitFirst()
        }

    @Test
    fun `success case`() =
        runBlocking {
            assertEquals(true, eventsDao.isNoEvents())
            val event = userRegistered(1000)

            eventStorePublisher.publish(event)

            assertEquals(false, eventsDao.isNoEvents())
            coVerify(exactly = 1) { userProjector.handleUserRegistered(any(), any()) }
        }

    @Test
    fun `exception in projector rollbacks transaction on spring data error`() =
        runBlocking {
            assertEquals(true, eventsDao.isNoEvents())
            val event = userRegistered(2000)
            coEvery { userProjector.handleUserRegistered(any(), any()) } coAnswers {
                val u1 = UserEntity()
                userRepository.save(u1)
            }

            assertThrows<DataIntegrityViolationException> { eventStorePublisher.publish(event) }

            assertEquals(true, eventsDao.isNoEvents())
            coVerify(exactly = 1) { userProjector.handleUserRegistered(any(), any()) }
        }

    @Test
    fun `exception in projector rollbacks transaction`() =
        runBlocking {
            assertEquals(true, eventsDao.isNoEvents())
            val event = userRegistered(2000)
            coEvery {
                userProjector.handleUserRegistered(any(), any())
            } throws IllegalStateException("error simulation")

            assertThrows<InvocationTargetException> { eventStorePublisher.publish(event) }

            assertEquals(true, eventsDao.isNoEvents())
            coVerify(exactly = 1) { userProjector.handleUserRegistered(any(), any()) }
        }

    @Test
    fun `exception in reactor dont rollbacks transaction`() =
        runBlocking {
            assertEquals(true, eventsDao.isNoEvents())
            val event = userRegistered(3000)
            coEvery { userRegisteredEmailReactor.handle(any()) } throws IllegalStateException("error simulation")

            eventStorePublisher.publish(event)

            assertEquals(false, eventsDao.isNoEvents())
            coVerify(exactly = 1) { userRegisteredEmailReactor.handle(any()) }
        }

    @Test
    fun `exception in one event stops processing of batch but dont rollback`() =
        runBlocking {
            assertEquals(true, eventsDao.isNoEvents())
            val event1 = userRegistered(4000)
            val event2 = userRegistered(5000)
            val event3 = userRegistered(6000)
            val events =
                listOf(
                    event1 to EventMetadata(),
                    event2 to EventMetadata(),
                    event3 to EventMetadata(),
                )
            coEvery {
                userProjector.handleUserRegistered(event2, any())
            } throws IllegalStateException("error simulation")

            assertThrows<InvocationTargetException> { eventStorePublisher.publish(events) }

            val eventsCount = eventsDao.eventsCount()
            assertEquals(1, eventsCount)
            coVerify(exactly = 2) { userProjector.handleUserRegistered(any(), any()) }
        }
}
