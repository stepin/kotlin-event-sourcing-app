package name.stepin.es.store

import name.stepin.db.dao.EventDao
import name.stepin.es.processor.InlineProcessor
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class EventStorePublisherImpl(
    private val eventMapper: EventMapper,
    private val eventDao: EventDao,
    private val inlineProcessor: InlineProcessor,
    @Lazy private val eventsStore: EventStorePublisher,
) : EventStorePublisher {

    override suspend fun publish(events: List<DomainEventWithMeta>, skipReactor: Boolean): List<UUID> {
        val ids = ArrayList<UUID>(events.size)
        for (event in events) {
            val id = eventsStore.publish(event.first, event.second, skipReactor)
            ids.add(id)
        }
        return ids
    }

    /**
     * Return id can be used in queries to wait until read model is consistent.
     */
    @Transactional
    override suspend fun publish(event: DomainEvent, meta: EventMetadata, skipReactor: Boolean): UUID {
        val eventDb = eventDao.newRecord()
        eventMapper.toRecord(eventDb, event, meta)
        eventDb.store()
        val guid = eventDb.guid!!

        inlineProcessor.process(event, meta, skipReactor)

        return guid
    }
}
