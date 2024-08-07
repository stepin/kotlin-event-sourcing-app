package name.stepin.es.store

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import name.stepin.db.dao.EventDao
import name.stepin.db.sql.tables.references.EVENTS
import name.stepin.exception.EntityNotFoundException
import org.jooq.impl.DSL.noCondition
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
class EventStoreReaderImpl(
    private val eventMapper: EventMapper,
    private val eventDao: EventDao,
) : EventStoreReader {
    override fun <T : DomainEvent> findEventsSinceId(
        eventIdFrom: Long,
        aggregator: String?,
        aggregatorGuid: UUID?,
        accountGuid: AccountGuid?,
        eventTypes: List<String>?,
        maxBatchSize: Int?,
    ): Flow<DomainEventWithIdAndMeta<T>> {
        val mainCondition = EVENTS.ID.gt(eventIdFrom)

        return eventDao.getEventsListOnCondition(
            mainCondition = mainCondition,
            accountGuid = accountGuid,
            aggregator = aggregator,
            aggregatorGuid = aggregatorGuid,
            eventTypes = eventTypes,
            maxBatchSize = maxBatchSize,
        )
            .map {
                eventMapper.toDomainEventWithIdAndMeta(it.value1())
            }
    }

    override fun <T : DomainEvent> findEventsSinceGuid(
        eventGuidFrom: UUID,
        aggregator: String?,
        aggregatorGuid: UUID?,
        accountGuid: AccountGuid?,
        eventTypes: List<String>?,
        maxBatchSize: Int?,
    ): Flow<DomainEventWithIdAndMeta<T>> =
        flow {
            val id =
                eventDao.byGuid(eventGuidFrom)?.id
                    ?: throw EntityNotFoundException()

            return@flow emitAll(
                findEventsSinceId(id, aggregator, aggregatorGuid, accountGuid, eventTypes, maxBatchSize),
            )
        }

    override fun <T : DomainEvent> findEventsSinceDate(
        date: LocalDateTime,
        aggregator: String?,
        aggregatorGuid: UUID?,
        accountGuid: AccountGuid?,
        eventTypes: List<String>?,
        maxBatchSize: Int?,
    ): Flow<DomainEventWithIdAndMeta<T>> {
        val mainCondition = EVENTS.CREATED_AT.gt(date)

        return eventDao.getEventsListOnCondition(
            mainCondition = mainCondition,
            accountGuid = accountGuid,
            aggregator = aggregator,
            aggregatorGuid = aggregatorGuid,
            eventTypes = eventTypes,
            maxBatchSize = maxBatchSize,
        )
            .map {
                eventMapper.toDomainEventWithIdAndMeta(it.value1())
            }
    }

    override fun <T : DomainEvent> findEvents(
        aggregator: String?,
        aggregatorGuid: UUID?,
        accountGuid: AccountGuid?,
        eventTypes: List<String>?,
        maxBatchSize: Int?,
    ): Flow<DomainEventWithIdAndMeta<T>> {
        val mainCondition = noCondition()

        return eventDao.getEventsListOnCondition(
            mainCondition = mainCondition,
            accountGuid = accountGuid,
            aggregator = aggregator,
            aggregatorGuid = aggregatorGuid,
            eventTypes = eventTypes,
            maxBatchSize = maxBatchSize,
        )
            .map {
                eventMapper.toDomainEventWithIdAndMeta(it.value1())
            }
    }
}
