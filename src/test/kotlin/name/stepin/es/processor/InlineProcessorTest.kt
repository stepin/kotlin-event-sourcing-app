package name.stepin.es.processor

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import name.stepin.domain.user.event.UserEvent
import name.stepin.es.handler.ProjectorRepository
import name.stepin.es.handler.ReactorRepository
import name.stepin.es.store.DomainEvent
import name.stepin.es.store.EventMetadata
import name.stepin.es.store.EventStoreReader
import name.stepin.fixture.EventsFactory.flow3events
import name.stepin.fixture.EventsFactory.userRegistered
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class InlineProcessorTest {
    private lateinit var service: InlineProcessor

    @MockK
    lateinit var eventStoreReader: EventStoreReader

    @MockK
    lateinit var projectors: ProjectorRepository

    @MockK
    lateinit var reactors: ReactorRepository

    @MockK
    lateinit var meterRegistry: MeterRegistry

    @BeforeEach
    fun setUp() {
        service = InlineProcessor(eventStoreReader, projectors, reactors, meterRegistry)
    }

    @AfterEach
    fun tearDown() {
        confirmVerified(eventStoreReader, projectors, reactors, meterRegistry)
    }

    @Test
    fun `empty case`() = runBlocking {
        coEvery {
            eventStoreReader.findEventsSinceId<DomainEvent>(0, null, null, null, null, null)
        } returns emptyFlow()

        service.replayEvents(0, -1)

        coVerify(exactly = 1) {
            eventStoreReader.findEventsSinceId<DomainEvent>(0, null, null, null, null, null)
        }
    }

    @Test
    fun `main case`() = runBlocking {
        coEvery {
            eventStoreReader.findEventsSinceId<UserEvent>(1, null, null, null, null, null)
        } returns flow3events()
        val serviceMock = spyk(service)
        coEvery { serviceMock.process(any(), any(), false) } returns Unit

        serviceMock.replayEvents(1, 2, false)

        coVerify(exactly = 1) {
            eventStoreReader.findEventsSinceId<UserEvent>(1, null, null, null, null, null)
        }
        coVerify(exactly = 2) { serviceMock.process(any(), any(), false) }
    }

    @Test
    fun `main case alternative`() = runBlocking {
        coEvery {
            eventStoreReader.findEventsSinceId<UserEvent>(0, null, null, null, null, null)
        } returns flow3events()
        val serviceMock = spyk(service)
        coEvery { serviceMock.process(any(), any(), true) } returns Unit

        serviceMock.replayEvents(0, -1, true)

        coVerify(exactly = 1) {
            eventStoreReader.findEventsSinceId<UserEvent>(0, null, null, null, null, null)
        }
        coVerify(exactly = 3) { serviceMock.process(any(), any(), true) }
    }

    @Test
    fun `process skip case`() = runBlocking {
        val event = userRegistered(1)
        val meta = EventMetadata(skip = true)

        service.process(event, meta, false)
    }

    @Test
    fun `process skip reactors`() = runBlocking {
        val event = userRegistered(1)
        val meta = EventMetadata()
        coEvery { projectors.process(event, meta) } returns Unit
        val mockCounter = mockk<Counter>()
        every {
            mockCounter.increment()
        } returns Unit
        every {
            meterRegistry.counter("events_counter", listOf(Tag.of("eventType", event.eventType)))
        } returns mockCounter

        service.process(event, meta, true)

        coVerify(exactly = 1) { projectors.process(event, meta) }
        verify(exactly = 1) { mockCounter.increment() }
        verify(exactly = 1) { meterRegistry.counter("events_counter", listOf(Tag.of("eventType", event.eventType))) }
    }

    @Test
    fun `process with reactors`() = runBlocking {
        val event = userRegistered(1)
        val meta = EventMetadata()
        coEvery { projectors.process(event, meta) } returns Unit
        coEvery { reactors.process(event, meta) } returns Unit
        val mockCounter = mockk<Counter>()
        every {
            mockCounter.increment()
        } returns Unit
        every {
            meterRegistry.counter("events_counter", listOf(Tag.of("eventType", event.eventType)))
        } returns mockCounter

        service.process(event, meta)

        coVerify(exactly = 1) { projectors.process(event, meta) }
        coVerify(exactly = 1) { reactors.process(event, meta) }
        verify(exactly = 1) { mockCounter.increment() }
        verify(exactly = 1) { meterRegistry.counter("events_counter", listOf(Tag.of("eventType", event.eventType))) }
    }
}
