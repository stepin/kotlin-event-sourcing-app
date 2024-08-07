package name.stepin.es.store

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import name.stepin.db.sql.tables.records.EventsRecord
import name.stepin.domain.user.event.UserMetaUpdated
import name.stepin.domain.user.event.UserRegistered
import name.stepin.es.handler.ReflectionHelper
import name.stepin.fixture.EventsFactory.userMetaUpdated
import org.intellij.lang.annotations.Language
import org.jooq.JSONB
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class EventMapperTest {
    private lateinit var service: EventMapper
    private val objectMapper: ObjectMapper =
        ObjectMapper().registerModule(KotlinModule.Builder().build()).apply {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }

    @MockK
    lateinit var reflectionHelper: ReflectionHelper

    @BeforeEach
    fun setUp() {
        service = EventMapper(reflectionHelper, objectMapper)
    }

    @AfterEach
    fun tearDown() {
        confirmVerified(reflectionHelper)
    }

    @Test
    fun `toRecord main case`() {
        val event = userMetaUpdated(1)
        val meta = EventMetadata()

        @Language("JSON")
        val json =
            """
            {
                "firstName":"firstName1",
                "aggregatorType":"user",
                "eventTypeVersion":0,
                "displayName":"displayName1",
                "aggregatorGuid":"${event.aggregatorGuid}",
                "guid":"${event.guid}",
                "eventType":"UserMetaUpdated",
                "accountGuid":"${event.accountGuid}",
                "secondName":"secondName1"
            }
            """.trimIndent()
        val expected =
            EventsRecord().apply {
                guid = event.guid
                accountGuid = event.accountGuid
                aggregator = event.aggregatorType
                aggregatorGuid = event.aggregatorGuid
                type = event.eventType
                version = event.eventTypeVersion
                createdAt = meta.createdAt
                comment = meta.comment
                creatorGuid = meta.creatorGuid
                skip = meta.skip
                body = JSONB.valueOf(json)
            }

        val actual = service.toRecord(event, meta)

        assertEquals(expected.guid, actual.guid)
        assertEquals(expected.accountGuid, actual.accountGuid)
        assertEquals(expected.aggregator, actual.aggregator)
        assertEquals(expected.aggregatorGuid, actual.aggregatorGuid)
        assertEquals(expected.type, actual.type)
        assertEquals(expected.version, actual.version)
        assertEquals(expected.createdAt, actual.createdAt)
        assertEquals(expected.comment, actual.comment)
        assertEquals(expected.creatorGuid, actual.creatorGuid)
        assertEquals(expected.skip, actual.skip)
        assertEquals(expected.body, actual.body)
    }

    @Test
    fun `toDomainEventWithIdAndMeta main case`() {
        val event = userMetaUpdated(1)
        val meta = EventMetadata()

        @Language("JSON")
        val json =
            """
            {
                "firstName":"firstName1",
                "aggregatorType":"user",
                "eventTypeVersion":0,
                "displayName":"displayName1",
                "aggregatorGuid":"${event.aggregatorGuid}",
                "guid":"${event.guid}",
                "eventType":"UserMetaUpdated",
                "accountGuid":"${event.accountGuid}",
                "secondName":"secondName1"
            }
            """.trimIndent()
        val record =
            EventsRecord().apply {
                id = 1L
                guid = event.guid
                accountGuid = event.accountGuid
                aggregator = event.aggregatorType
                aggregatorGuid = event.aggregatorGuid
                type = event.eventType
                version = event.eventTypeVersion
                createdAt = meta.createdAt
                comment = meta.comment
                creatorGuid = meta.creatorGuid
                skip = meta.skip
                body = JSONB.valueOf(json)
            }
        every { reflectionHelper.eventsToClass() } answers {
            val map = HashMap<String, Class<*>>()
            map["UserRegistered"] = UserRegistered::class.java
            map["UserMetaUpdated"] = UserMetaUpdated::class.java
            map
        }

        service.initEvent2class()
        val actual = service.toDomainEventWithIdAndMeta<UserMetaUpdated>(record)

        assertEquals(1, actual.id)
        assertEquals(meta, actual.meta)
        assertEquals(event, actual.event)
        verify(exactly = 1) { reflectionHelper.eventsToClass() }
    }
}
