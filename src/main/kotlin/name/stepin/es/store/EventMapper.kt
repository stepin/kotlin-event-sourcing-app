package name.stepin.es.store

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import name.stepin.db.sql.tables.records.EventsRecord
import name.stepin.es.handler.ReflectionHelper
import org.jooq.JSONB
import org.springframework.stereotype.Service

@Service
class EventMapper(
    private val reflectionHelper: ReflectionHelper,
    private val objectMapper: ObjectMapper,
) {
    private lateinit var event2class: HashMap<String, Class<*>>

    @PostConstruct
    fun initEvent2class() {
        event2class = reflectionHelper.eventsToClass()
    }

    /**
     * For save operation.
     */
    fun toRecord(eventsRecord: EventsRecord, event: DomainEvent, meta: EventMetadata) {
        eventsRecord.guid = event.guid
        eventsRecord.accountGuid = event.accountGuid
        eventsRecord.aggregator = event.aggregatorType
        eventsRecord.aggregatorGuid = event.aggregatorGuid
        eventsRecord.type = event.eventType
        eventsRecord.version = event.eventTypeVersion
        eventsRecord.createdAt = meta.createdAt
        eventsRecord.comment = meta.comment
        eventsRecord.creatorGuid = meta.creatorGuid
        eventsRecord.skip = meta.skip

        val bodyStr = objectMapper.writeValueAsString(event)
        val body = JSONB.valueOf(bodyStr)
        eventsRecord.body = body
    }

    /**
     * For read operation.
     */
    fun <T : DomainEvent> toDomainEventWithIdAndMeta(eventRecord: EventsRecord): DomainEventWithIdAndMeta<T> {
        val id = eventRecord.id!!

        @Suppress("UNCHECKED_CAST")
        val type = event2class[eventRecord.type] as Class<out DomainEvent>
        val event = objectMapper.readValue(eventRecord.body.toString(), type)

        val meta = EventMetadata(
            creatorGuid = eventRecord.creatorGuid!!,
            createdAt = eventRecord.createdAt!!,
            comment = eventRecord.comment!!,
            skip = eventRecord.skip!!,
        )

        @Suppress("UNCHECKED_CAST")
        val eventWithType = event as? T
            ?: throw IllegalStateException("Incorrect event type: $event")

        return DomainEventWithIdAndMeta(id, eventWithType, meta)
    }
}
