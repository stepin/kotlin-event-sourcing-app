package name.stepin.es.store

import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.mono
import name.stepin.es.processor.InlineProcessor
import name.stepin.utils.coInsert
import org.jooq.DSLContext
import org.springframework.stereotype.Service
import java.util.*

@Service
class EventStorePublisherImpl(
    private val eventMapper: EventMapper,
    private val db: DSLContext,
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
        return db.transactionPublisher { config ->
            mono {
                val dsl = config.dsl()

                val eventDb = eventMapper.toRecord(event, meta)
                val guid = eventDb.guid!!

                dsl.coInsert(eventDb)

                inlineProcessor.process(event, meta, skipReactor)

                guid
            }
        }.awaitFirst()
    }
}
