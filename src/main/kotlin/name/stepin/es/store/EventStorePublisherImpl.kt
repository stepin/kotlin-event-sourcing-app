package name.stepin.es.store

import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.mono
import name.stepin.db.sql.tables.references.EVENTS
import name.stepin.es.processor.InlineProcessor
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.*

@Service
class EventStorePublisherImpl(
    private val eventMapper: EventMapper,
    @Qualifier("jdbcDb")
    private val jdbcDb: DSLContext,
    private val inlineProcessor: InlineProcessor,
) : EventStorePublisher {

    override suspend fun publish(events: List<DomainEventWithMeta>, skipReactor: Boolean): List<UUID> {
        val ids = ArrayList<UUID>(events.size)
        for (event in events) {
            val id = publish(event.first, event.second, skipReactor)
            ids.add(id)
        }
        return ids
    }

    /**
     * Return id can be used in queries to wait until read model is consistent.
     */
    override suspend fun publish(event: DomainEvent, meta: EventMetadata, skipReactor: Boolean): UUID {
        return jdbcDb.transactionPublisher { config ->
            val dsl = config.dsl()

            val eventDb = dsl.newRecord(EVENTS)
            eventMapper.toRecord(eventDb, event, meta)
            eventDb.store()
            val guid = eventDb.guid!!

            mono {
                inlineProcessor.process(event, meta, skipReactor)
            }
                .map { guid }
        }.awaitFirst()
    }
}
