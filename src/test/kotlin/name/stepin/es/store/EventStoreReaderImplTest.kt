package name.stepin.es.store

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import name.stepin.config.EventSourcingConfig
import name.stepin.db.dao.EventDao
import name.stepin.db.sql.tables.records.EventsRecord
import name.stepin.db.sql.tables.references.EVENTS
import name.stepin.es.handler.ReflectionHelper
import name.stepin.exception.EntityNotFoundException
import name.stepin.fixture.EventsFactory.userRegistered
import org.jooq.JSONB
import org.jooq.Record1
import org.jooq.impl.DSL
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockKExtension::class)
class EventStoreReaderImplTest {
    private lateinit var service: EventStoreReader
    private lateinit var eventMapper: EventMapper
    private val objectMapper: ObjectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build()).apply {
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    @MockK
    lateinit var eventDao: EventDao

    @BeforeEach
    fun setUp() {
        eventMapper = EventMapper(ReflectionHelper(EventSourcingConfig(eventsPackage = "name.stepin")), objectMapper)
        eventMapper.initEvent2class()

        service = EventStoreReaderImpl(eventMapper, eventDao)
    }

    @AfterEach
    fun tearDown() {
        confirmVerified(eventDao)
    }

    private fun createEventRecord(
        id: Long,
        accountGuid: AccountGuid = UUID.randomUUID(),
        aggregator: String = "user",
        aggregatorGuid: UUID = UUID.randomUUID(),
        type: String? = null,
    ): EventsRecord {
        val e = EventsRecord()
        e.id = id
        e.guid = UUID.randomUUID()
        e.accountGuid = accountGuid
        e.aggregator = aggregator
        e.aggregatorGuid = aggregatorGuid
        e.creatorGuid = UUID.randomUUID()
        e.type = type ?: "UserCreated$id"
        e.version = 0
        e.skip = false
        e.body = JSONB.valueOf("{\"user\": $id}")
        e.comment = "Just test record$id"
        e.createdAt = LocalDateTime.now()
        return e
    }

    @Test
    fun `findEventsSinceId defaults case`() = runBlocking {
        val expectedEvent = userRegistered(1)
        val eventRecord1 = createEventRecord(1, type = "UserRegistered").apply {
            body = JSONB.valueOf(
                """
                {"guid": "${expectedEvent.guid}", "email": "email1@example.com", "eventType": "UserRegistered", "firstName": "firstName1", "secondName": "secondName1", "accountGuid": "${expectedEvent.accountGuid}", "displayName": "displayName1", "aggregatorGuid": "${expectedEvent.aggregatorGuid}", "aggregatorType": "user", "eventTypeVersion": 3}
                """.trimIndent(),
            )
        }
        val expectedMeta = EventMetadata(
            creatorGuid = eventRecord1.creatorGuid!!,
            createdAt = eventRecord1.createdAt!!,
            comment = eventRecord1.comment!!,
        )
        val record1 = mockk<Record1<EventsRecord>>()
        every { record1.value1() } returns eventRecord1
        coEvery {
            eventDao.getEventsListOnCondition(
                mainCondition = EVENTS.ID.gt(10),
                accountGuid = null,
                aggregator = null,
                aggregatorGuid = null,
                eventTypes = null,
                maxBatchSize = null,
            )
        } returns flowOf(record1)

        val actual = service.findEventsSinceId<DomainEvent>(10)

        val list = actual.toList()
        assertEquals(1, list.size)
        val (actualId, actualEvent, actualMeta) = list.first()
        assertEquals(1, actualId)
        assertEquals(expectedEvent, actualEvent)
        assertEquals(expectedMeta, actualMeta)
        coVerify(exactly = 1) {
            eventDao.getEventsListOnCondition(
                mainCondition = EVENTS.ID.gt(10),
                accountGuid = null,
                aggregator = null,
                aggregatorGuid = null,
                eventTypes = null,
                maxBatchSize = null,
            )
        }
        verify(exactly = 1) { record1.value1() }
    }

    @Test
    fun `findEventsSinceId main case`() = runBlocking {
        val expectedEvent = userRegistered(1)
        val eventRecord1 = createEventRecord(1, type = "UserRegistered").apply {
            body = JSONB.valueOf(
                """
                {"guid": "${expectedEvent.guid}", "email": "email1@example.com", "eventType": "UserRegistered", "firstName": "firstName1", "secondName": "secondName1", "accountGuid": "${expectedEvent.accountGuid}", "displayName": "displayName1", "aggregatorGuid": "${expectedEvent.aggregatorGuid}", "aggregatorType": "user", "eventTypeVersion": 3}
                """.trimIndent(),
            )
        }
        val expectedMeta = EventMetadata(
            creatorGuid = eventRecord1.creatorGuid!!,
            createdAt = eventRecord1.createdAt!!,
            comment = eventRecord1.comment!!,
        )
        val findAccountGuid = UUID.randomUUID()
        val findAggregator = "user"
        val findUserGuid = UUID.randomUUID()
        val findEventTypes = listOf("event")
        val maxBatchSize = 123
        val record1 = mockk<Record1<EventsRecord>>()
        every { record1.value1() } returns eventRecord1
        coEvery {
            eventDao.getEventsListOnCondition(
                mainCondition = EVENTS.ID.gt(10),
                accountGuid = findAccountGuid,
                aggregator = findAggregator,
                aggregatorGuid = findUserGuid,
                eventTypes = findEventTypes,
                maxBatchSize = maxBatchSize,
            )
        } returns flowOf(record1)

        val actual = service.findEventsSinceId<DomainEvent>(
            eventIdFrom = 10,
            aggregator = findAggregator,
            aggregatorGuid = findUserGuid,
            accountGuid = findAccountGuid,
            eventTypes = findEventTypes,
            maxBatchSize = maxBatchSize,
        )

        val list = actual.toList()
        assertEquals(1, list.size)
        val (actualId, actualEvent, actualMeta) = list.first()
        assertEquals(1, actualId)
        assertEquals(expectedEvent, actualEvent)
        assertEquals(expectedMeta, actualMeta)
        coVerify(exactly = 1) {
            eventDao.getEventsListOnCondition(
                mainCondition = EVENTS.ID.gt(10),
                accountGuid = findAccountGuid,
                aggregator = findAggregator,
                aggregatorGuid = findUserGuid,
                eventTypes = findEventTypes,
                maxBatchSize = maxBatchSize,
            )
        }
        verify(exactly = 1) { record1.value1() }
    }

    @Test
    fun `findEventsSinceGuid not found case`() = runBlocking {
        val findFromGuid = UUID.randomUUID()
        coEvery { eventDao.byGuid(findFromGuid) } returns null

        val exception = assertThrows<EntityNotFoundException> {
            val actual = service.findEventsSinceGuid<DomainEvent>(findFromGuid)
            actual.toList()
        }

        assertEquals(null, exception.message)
        coVerify(exactly = 1) { eventDao.byGuid(findFromGuid) }
    }

    @Test
    fun `findEventsSinceGuid defaults case`() = runBlocking {
        val expectedEvent = userRegistered(1)
        val eventRecord1 = createEventRecord(1, type = "UserRegistered").apply {
            body = JSONB.valueOf(
                """
                {"guid": "${expectedEvent.guid}", "email": "email1@example.com", "eventType": "UserRegistered", "firstName": "firstName1", "secondName": "secondName1", "accountGuid": "${expectedEvent.accountGuid}", "displayName": "displayName1", "aggregatorGuid": "${expectedEvent.aggregatorGuid}", "aggregatorType": "user", "eventTypeVersion": 3}
                """.trimIndent(),
            )
        }
        val expectedMeta = EventMetadata(
            creatorGuid = eventRecord1.creatorGuid!!,
            createdAt = eventRecord1.createdAt!!,
            comment = eventRecord1.comment!!,
        )
        val findFromGuid = UUID.randomUUID()
        val record1 = mockk<Record1<EventsRecord>>()
        every { record1.value1() } returns eventRecord1
        coEvery { eventDao.byGuid(findFromGuid) } returns EventsRecord().apply { id = 10 }
        coEvery {
            eventDao.getEventsListOnCondition(
                mainCondition = EVENTS.ID.gt(10),
                accountGuid = null,
                aggregator = null,
                aggregatorGuid = null,
                eventTypes = null,
                maxBatchSize = null,
            )
        } returns flowOf(record1)

        val actual = service.findEventsSinceGuid<DomainEvent>(findFromGuid)

        val list = actual.toList()
        assertEquals(1, list.size)
        val (actualId, actualEvent, actualMeta) = list.first()
        assertEquals(1, actualId)
        assertEquals(expectedEvent, actualEvent)
        assertEquals(expectedMeta, actualMeta)
        coVerify(exactly = 1) { eventDao.byGuid(findFromGuid) }
        coVerify(exactly = 1) {
            eventDao.getEventsListOnCondition(
                mainCondition = EVENTS.ID.gt(10),
                accountGuid = null,
                aggregator = null,
                aggregatorGuid = null,
                eventTypes = null,
                maxBatchSize = null,
            )
        }
        verify(exactly = 1) { record1.value1() }
    }

    @Test
    fun `findEventsSinceGuid main case`() = runBlocking {
        val expectedEvent = userRegistered(1)
        val eventRecord1 = createEventRecord(1, type = "UserRegistered").apply {
            body = JSONB.valueOf(
                """
                {"guid": "${expectedEvent.guid}", "email": "email1@example.com", "eventType": "UserRegistered", "firstName": "firstName1", "secondName": "secondName1", "accountGuid": "${expectedEvent.accountGuid}", "displayName": "displayName1", "aggregatorGuid": "${expectedEvent.aggregatorGuid}", "aggregatorType": "user", "eventTypeVersion": 3}
                """.trimIndent(),
            )
        }
        val expectedMeta = EventMetadata(
            creatorGuid = eventRecord1.creatorGuid!!,
            createdAt = eventRecord1.createdAt!!,
            comment = eventRecord1.comment!!,
        )
        val findFromGuid = UUID.randomUUID()
        val findAccountGuid = UUID.randomUUID()
        val findAggregator = "user"
        val findUserGuid = UUID.randomUUID()
        val findEventTypes = listOf("event")
        val maxBatchSize = 123
        val record1 = mockk<Record1<EventsRecord>>()
        every { record1.value1() } returns eventRecord1
        coEvery { eventDao.byGuid(findFromGuid) } returns EventsRecord().apply { id = 10 }
        coEvery {
            eventDao.getEventsListOnCondition(
                mainCondition = EVENTS.ID.gt(10),
                accountGuid = findAccountGuid,
                aggregator = findAggregator,
                aggregatorGuid = findUserGuid,
                eventTypes = findEventTypes,
                maxBatchSize = maxBatchSize,
            )
        } returns flowOf(record1)

        val actual = service.findEventsSinceGuid<DomainEvent>(
            eventGuidFrom = findFromGuid,
            aggregator = findAggregator,
            aggregatorGuid = findUserGuid,
            accountGuid = findAccountGuid,
            eventTypes = findEventTypes,
            maxBatchSize = maxBatchSize,
        )

        val list = actual.toList()
        assertEquals(1, list.size)
        val (actualId, actualEvent, actualMeta) = list.first()
        assertEquals(1, actualId)
        assertEquals(expectedEvent, actualEvent)
        assertEquals(expectedMeta, actualMeta)
        coVerify(exactly = 1) { eventDao.byGuid(findFromGuid) }
        coVerify(exactly = 1) {
            eventDao.getEventsListOnCondition(
                mainCondition = EVENTS.ID.gt(10),
                accountGuid = findAccountGuid,
                aggregator = findAggregator,
                aggregatorGuid = findUserGuid,
                eventTypes = findEventTypes,
                maxBatchSize = maxBatchSize,
            )
        }
        verify(exactly = 1) { record1.value1() }
    }

    @Test
    fun `findEventsSinceDate defaults case`() = runBlocking {
        val expectedEvent = userRegistered(1)
        val eventRecord1 = createEventRecord(1, type = "UserRegistered").apply {
            body = JSONB.valueOf(
                """
                {"guid": "${expectedEvent.guid}", "email": "email1@example.com", "eventType": "UserRegistered", "firstName": "firstName1", "secondName": "secondName1", "accountGuid": "${expectedEvent.accountGuid}", "displayName": "displayName1", "aggregatorGuid": "${expectedEvent.aggregatorGuid}", "aggregatorType": "user", "eventTypeVersion": 3}
                """.trimIndent(),
            )
        }
        val expectedMeta = EventMetadata(
            creatorGuid = eventRecord1.creatorGuid!!,
            createdAt = eventRecord1.createdAt!!,
            comment = eventRecord1.comment!!,
        )
        val findFromDate = LocalDateTime.of(2023, 1, 1, 1, 1, 1)
        val record1 = mockk<Record1<EventsRecord>>()
        every { record1.value1() } returns eventRecord1
        coEvery {
            eventDao.getEventsListOnCondition(
                mainCondition = EVENTS.CREATED_AT.gt(findFromDate),
                accountGuid = null,
                aggregator = null,
                aggregatorGuid = null,
                eventTypes = null,
                maxBatchSize = null,
            )
        } returns flowOf(record1)

        val actual = service.findEventsSinceDate<DomainEvent>(findFromDate)

        val list = actual.toList()
        assertEquals(1, list.size)
        val (actualId, actualEvent, actualMeta) = list.first()
        assertEquals(1, actualId)
        assertEquals(expectedEvent, actualEvent)
        assertEquals(expectedMeta, actualMeta)
        coVerify(exactly = 1) {
            eventDao.getEventsListOnCondition(
                mainCondition = EVENTS.CREATED_AT.gt(findFromDate),
                accountGuid = null,
                aggregator = null,
                aggregatorGuid = null,
                eventTypes = null,
                maxBatchSize = null,
            )
        }
        verify(exactly = 1) { record1.value1() }
    }

    @Test
    fun `findEventsSinceDate main case`() = runBlocking {
        val expectedEvent = userRegistered(1)
        val eventRecord1 = createEventRecord(1, type = "UserRegistered").apply {
            body = JSONB.valueOf(
                """
                {"guid": "${expectedEvent.guid}", "email": "email1@example.com", "eventType": "UserRegistered", "firstName": "firstName1", "secondName": "secondName1", "accountGuid": "${expectedEvent.accountGuid}", "displayName": "displayName1", "aggregatorGuid": "${expectedEvent.aggregatorGuid}", "aggregatorType": "user", "eventTypeVersion": 3}
                """.trimIndent(),
            )
        }
        val expectedMeta = EventMetadata(
            creatorGuid = eventRecord1.creatorGuid!!,
            createdAt = eventRecord1.createdAt!!,
            comment = eventRecord1.comment!!,
        )
        val findFromDate = LocalDateTime.of(2023, 1, 1, 1, 1, 1)
        val findAccountGuid = UUID.randomUUID()
        val findAggregator = "user"
        val findUserGuid = UUID.randomUUID()
        val findEventTypes = listOf("event")
        val maxBatchSize = 123
        val record1 = mockk<Record1<EventsRecord>>()
        every { record1.value1() } returns eventRecord1
        coEvery {
            eventDao.getEventsListOnCondition(
                mainCondition = EVENTS.CREATED_AT.gt(findFromDate),
                accountGuid = findAccountGuid,
                aggregator = findAggregator,
                aggregatorGuid = findUserGuid,
                eventTypes = findEventTypes,
                maxBatchSize = maxBatchSize,
            )
        } returns flowOf(record1)

        val actual = service.findEventsSinceDate<DomainEvent>(
            date = findFromDate,
            aggregator = findAggregator,
            aggregatorGuid = findUserGuid,
            accountGuid = findAccountGuid,
            eventTypes = findEventTypes,
            maxBatchSize = maxBatchSize,
        )

        val list = actual.toList()
        assertEquals(1, list.size)
        val (actualId, actualEvent, actualMeta) = list.first()
        assertEquals(1, actualId)
        assertEquals(expectedEvent, actualEvent)
        assertEquals(expectedMeta, actualMeta)
        coVerify(exactly = 1) {
            eventDao.getEventsListOnCondition(
                mainCondition = EVENTS.CREATED_AT.gt(findFromDate),
                accountGuid = findAccountGuid,
                aggregator = findAggregator,
                aggregatorGuid = findUserGuid,
                eventTypes = findEventTypes,
                maxBatchSize = maxBatchSize,
            )
        }
        verify(exactly = 1) { record1.value1() }
    }

    @Test
    fun `findEvents defaults case`() = runBlocking {
        val expectedEvent = userRegistered(1)
        val eventRecord1 = createEventRecord(1, type = "UserRegistered").apply {
            body = JSONB.valueOf(
                """
                {"guid": "${expectedEvent.guid}", "email": "email1@example.com", "eventType": "UserRegistered", "firstName": "firstName1", "secondName": "secondName1", "accountGuid": "${expectedEvent.accountGuid}", "displayName": "displayName1", "aggregatorGuid": "${expectedEvent.aggregatorGuid}", "aggregatorType": "user", "eventTypeVersion": 3}
                """.trimIndent(),
            )
        }
        val expectedMeta = EventMetadata(
            creatorGuid = eventRecord1.creatorGuid!!,
            createdAt = eventRecord1.createdAt!!,
            comment = eventRecord1.comment!!,
        )
        val record1 = mockk<Record1<EventsRecord>>()
        every { record1.value1() } returns eventRecord1
        coEvery {
            eventDao.getEventsListOnCondition(
                mainCondition = DSL.noCondition(),
                accountGuid = null,
                aggregator = null,
                aggregatorGuid = null,
                eventTypes = null,
                maxBatchSize = null,
            )
        } returns flowOf(record1)

        val actual = service.findEvents<DomainEvent>()

        val list = actual.toList()
        assertEquals(1, list.size)
        val (actualId, actualEvent, actualMeta) = list.first()
        assertEquals(1, actualId)
        assertEquals(expectedEvent, actualEvent)
        assertEquals(expectedMeta, actualMeta)
        coVerify(exactly = 1) {
            eventDao.getEventsListOnCondition(
                mainCondition = DSL.noCondition(),
                accountGuid = null,
                aggregator = null,
                aggregatorGuid = null,
                eventTypes = null,
                maxBatchSize = null,
            )
        }
        verify(exactly = 1) { record1.value1() }
    }

    @Test
    fun `findEvents main case`() = runBlocking {
        val expectedEvent = userRegistered(1)
        val eventRecord1 = createEventRecord(1, type = "UserRegistered").apply {
            body = JSONB.valueOf(
                """
                {"guid": "${expectedEvent.guid}", "email": "email1@example.com", "eventType": "UserRegistered", "firstName": "firstName1", "secondName": "secondName1", "accountGuid": "${expectedEvent.accountGuid}", "displayName": "displayName1", "aggregatorGuid": "${expectedEvent.aggregatorGuid}", "aggregatorType": "user", "eventTypeVersion": 3}
                """.trimIndent(),
            )
        }
        val expectedMeta = EventMetadata(
            creatorGuid = eventRecord1.creatorGuid!!,
            createdAt = eventRecord1.createdAt!!,
            comment = eventRecord1.comment!!,
        )
        val findAccountGuid = UUID.randomUUID()
        val findAggregator = "user"
        val findUserGuid = UUID.randomUUID()
        val findEventTypes = listOf("event")
        val maxBatchSize = 123
        val record1 = mockk<Record1<EventsRecord>>()
        every { record1.value1() } returns eventRecord1
        coEvery {
            eventDao.getEventsListOnCondition(
                mainCondition = DSL.noCondition(),
                accountGuid = findAccountGuid,
                aggregator = findAggregator,
                aggregatorGuid = findUserGuid,
                eventTypes = findEventTypes,
                maxBatchSize = maxBatchSize,
            )
        } returns flowOf(record1)

        val actual = service.findEvents<DomainEvent>(
            aggregator = findAggregator,
            aggregatorGuid = findUserGuid,
            accountGuid = findAccountGuid,
            eventTypes = findEventTypes,
            maxBatchSize = maxBatchSize,
        )

        val list = actual.toList()
        assertEquals(1, list.size)
        val (actualId, actualEvent, actualMeta) = list.first()
        assertEquals(1, actualId)
        assertEquals(expectedEvent, actualEvent)
        assertEquals(expectedMeta, actualMeta)
        coVerify(exactly = 1) {
            eventDao.getEventsListOnCondition(
                mainCondition = DSL.noCondition(),
                accountGuid = findAccountGuid,
                aggregator = findAggregator,
                aggregatorGuid = findUserGuid,
                eventTypes = findEventTypes,
                maxBatchSize = maxBatchSize,
            )
        }
        verify(exactly = 1) { record1.value1() }
    }
}
