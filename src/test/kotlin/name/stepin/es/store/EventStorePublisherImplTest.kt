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
import name.stepin.db.dao.EventDao
import name.stepin.db.sql.tables.records.EventsRecord
import name.stepin.es.handler.ReflectionHelper
import name.stepin.es.processor.InlineProcessor
import name.stepin.fixture.EventsFactory.flow3events
import name.stepin.fixture.EventsFactory.userRegistered
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class EventStorePublisherImplTest {
    private lateinit var service: EventStorePublisher
    private lateinit var eventMapper: EventMapper
    private val objectMapper: ObjectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build()).apply {
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    @MockK
    lateinit var eventDao: EventDao

    @MockK
    lateinit var inlineProcessor: InlineProcessor

    @MockK
    lateinit var eventsStore: EventStorePublisher

    @BeforeEach
    fun setUp() {
        eventMapper = EventMapper(ReflectionHelper(EventSourcingConfig(eventsPackage = "name.stepin")), objectMapper)
        eventMapper.initEvent2class()

        service = EventStorePublisherImpl(eventMapper, eventDao, inlineProcessor, eventsStore)
    }

    @AfterEach
    fun tearDown() {
        confirmVerified(eventDao, inlineProcessor, eventsStore)
    }

    @Test
    fun `publish list default case`() = runBlocking {
        val events: List<DomainEventWithMeta> = flow3events().toList().map { it.event to it.meta }
        val expected = events.map { it.first.guid }
        coEvery { eventsStore.publish(any<DomainEvent>(), any(), false) } answers {
            val event: DomainEvent = firstArg()
            event.guid
        }

        val actual = service.publish(events)

        assertEquals(expected, actual)
        coVerify(exactly = 3) { eventsStore.publish(any<DomainEvent>(), any(), false) }
    }

    @Test
    fun `publish list skip reactor case`() = runBlocking {
        val events: List<DomainEventWithMeta> = flow3events().toList().map { it.event to it.meta }
        val expected = events.map { it.first.guid }
        coEvery { eventsStore.publish(any<DomainEvent>(), any(), true) } answers {
            val event: DomainEvent = firstArg()
            event.guid
        }

        val actual = service.publish(events, true)

        assertEquals(expected, actual)
        coVerify(exactly = 3) { eventsStore.publish(any<DomainEvent>(), any(), true) }
    }

    @Test
    fun `publish one default case`() = runBlocking {
        val event = userRegistered(1)
        val meta = EventMetadata()
        val record = spyk(EventsRecord())
        every { eventDao.newRecord() } returns record
        every { record.store() } returns 10
        coEvery { inlineProcessor.process(event, meta, false) } returns Unit

        val actual = service.publish(event, meta)

        assertEquals(event.guid, actual)
        verify(exactly = 1) { eventDao.newRecord() }
        verify(exactly = 1) { record.store() }
        coVerify(exactly = 1) { inlineProcessor.process(event, meta, false) }
    }

    @Test
    fun `publish one skip reactor case`() = runBlocking {
        val event = userRegistered(1)
        val meta = EventMetadata()
        val record = spyk(EventsRecord())
        every { eventDao.newRecord() } returns record
        every { record.store() } returns 10
        coEvery { inlineProcessor.process(event, meta, true) } returns Unit

        val actual = service.publish(event, meta, true)

        assertEquals(event.guid, actual)
        verify(exactly = 1) { eventDao.newRecord() }
        verify(exactly = 1) { record.store() }
        coVerify(exactly = 1) { inlineProcessor.process(event, meta, true) }
    }
}
