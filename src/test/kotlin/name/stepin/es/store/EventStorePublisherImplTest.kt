package name.stepin.es.store

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import name.stepin.config.EventSourcingConfig
import name.stepin.db.sql.tables.records.EventsRecord
import name.stepin.es.handler.ReflectionHelper
import name.stepin.es.processor.InlineProcessor
import name.stepin.fixture.EventsFactory.flow3events
import name.stepin.fixture.EventsFactory.userRegistered
import name.stepin.utils.coInsert
import org.jooq.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
class EventStorePublisherImplTest {
    private lateinit var service: EventStorePublisher
    private lateinit var eventMapper: EventMapper
    private val objectMapper: ObjectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build()).apply {
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    @MockK
    lateinit var db: DSLContext

    @MockK
    lateinit var inlineProcessor: InlineProcessor

    @BeforeEach
    fun setUp() {
        eventMapper = EventMapper(ReflectionHelper(EventSourcingConfig(eventsPackage = "name.stepin")), objectMapper)
        eventMapper.initEvent2class()

        service = EventStorePublisherImpl(eventMapper, db, inlineProcessor)
    }

    @AfterEach
    fun tearDown() {
        confirmVerified(db, inlineProcessor)
    }

    @Test
    fun `publish list default case`() = runBlocking {
        val events: List<DomainEventWithMeta> = flow3events().toList().map { it.event to it.meta }
        val expected = events.map { it.first.guid }
        val spyService = spyk(service)
        coEvery { spyService.publish(any<DomainEvent>(), any(), false) } answers {
            val event: DomainEvent = firstArg()
            event.guid
        }

        val actual = spyService.publish(events)

        assertEquals(expected, actual)
        coVerify(exactly = 3) { spyService.publish(any<DomainEvent>(), any(), false) }
    }

    @Test
    fun `publish list skip reactor case`() = runBlocking {
        val events: List<DomainEventWithMeta> = flow3events().toList().map { it.event to it.meta }
        val expected = events.map { it.first.guid }
        val spyService = spyk(service)
        coEvery { spyService.publish(any<DomainEvent>(), any(), true) } answers {
            val event: DomainEvent = firstArg()
            event.guid
        }

        val actual = spyService.publish(events, true)

        assertEquals(expected, actual)
        coVerify(exactly = 3) { spyService.publish(any<DomainEvent>(), any(), true) }
    }

    @Test
    fun `publish one default case`() = runBlocking {
        val event = userRegistered(1)
        val meta = EventMetadata()
        val dsl = mockk<DSLContext>()
        val configuration = mockk<Configuration>()
        mockkStatic("name.stepin.utils.JooqKotlin") {
            coEvery { dsl.coInsert(any<EventsRecord>()) } returns 10
            every { db.transactionPublisher<UUID>(any()) } answers {
                val block = firstArg<TransactionalPublishable<UUID>>()
                val reactivePublisher = block.run(configuration)
                val jooqPublisher = Publisher { reactivePublisher.subscribe(it) }
                jooqPublisher
            }
            every { configuration.dsl() } returns dsl
            coEvery { inlineProcessor.process(event, meta, false) } returns Unit

            val actual = service.publish(event, meta)

            assertEquals(event.guid, actual)
            verify(exactly = 1) { db.transactionPublisher<UUID>(any()) }
            verify(exactly = 1) { configuration.dsl() }
            coVerify(exactly = 1) { dsl.coInsert(any<EventsRecord>()) }
            coVerify(exactly = 1) { inlineProcessor.process(event, meta, false) }
        }
    }

    @Test
    fun `publish one skip reactor case`() = runBlocking {
        val event = userRegistered(1)
        val meta = EventMetadata()
        val dsl = mockk<DSLContext>()
        val configuration = mockk<Configuration>()
        mockkStatic("name.stepin.utils.JooqKotlin") {
            every { db.transactionPublisher<UUID>(any()) } answers {
                val block = firstArg<TransactionalPublishable<UUID>>()
                val reactivePublisher = block.run(configuration)
                val jooqPublisher = Publisher { reactivePublisher.subscribe(it) }
                jooqPublisher
            }
            every { configuration.dsl() } returns dsl
            coEvery { dsl.coInsert(any<EventsRecord>()) } returns 1
            coEvery { inlineProcessor.process(event, meta, true) } returns Unit

            val actual = service.publish(event, meta, true)

            assertEquals(event.guid, actual)
            verify(exactly = 1) { db.transactionPublisher<UUID>(any()) }
            verify(exactly = 1) { configuration.dsl() }
            coVerify(exactly = 1) { dsl.coInsert(any<EventsRecord>()) }
            coVerify(exactly = 1) { inlineProcessor.process(event, meta, true) }
        }
    }
}
