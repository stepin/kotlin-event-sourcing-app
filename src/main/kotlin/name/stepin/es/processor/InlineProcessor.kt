package name.stepin.es.processor

import io.micrometer.core.annotation.Timed
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import name.stepin.es.handler.ProjectorRepository
import name.stepin.es.handler.ReactorRepository
import name.stepin.es.store.DomainEvent
import name.stepin.es.store.DomainEventWithIdAndMeta
import name.stepin.es.store.EventMetadata
import name.stepin.es.store.EventStoreReader
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.stereotype.Service

@Service
class InlineProcessor(
    private val eventStoreReader: EventStoreReader,
    private val projectors: ProjectorRepository,
    private val reactors: ReactorRepository,
    private val meterRegistry: MeterRegistry,
) {
    companion object : Logging

    /**
     * Main use case is to replay all events from scratch to the new DB (projection).
     */
    suspend fun replayEvents(
        from: Long,
        to: Long,
        skipReactor: Boolean = false,
    ) {
        logger.info { "replayEvents $from $to $skipReactor" }
        val start = System.currentTimeMillis()

        val events = eventStoreReader.findEventsSinceId<DomainEvent>(from)
        events.collect { (id, event, meta): DomainEventWithIdAndMeta<DomainEvent> ->
            if (to != -1L && id > to) return@collect
            process(event, meta, skipReactor)
        }
        val duration = System.currentTimeMillis() - start
        logger.debug { "replayEvents ${duration}ms" }
    }

    /**
     * Main use case for new events.
     */
    @Timed
    suspend fun process(
        event: DomainEvent,
        meta: EventMetadata,
        skipReactor: Boolean = false,
    ) {
        logger.info { "Processing $event $meta $skipReactor" }
        if (meta.skip) {
            return
        }
        val start = System.currentTimeMillis()

        // NOTE: we expect that all events are created from this app. So, we don't recheck if new event shows in DB
        projectors.process(event, meta)
        // NOTE: built-in reactors in general use projection as state. So, order of projections/reactors is important.
        if (!skipReactor) {
            reactors.process(event, meta)
        }

        meterRegistry.counter("events_counter", listOf(Tag.of("eventType", event.eventType))).increment()
        val duration = System.currentTimeMillis() - start
        logger.debug { "Processed event ${event.eventType} ${event.guid} in ${duration}ms" }
    }
}
