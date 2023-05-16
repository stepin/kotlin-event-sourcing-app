package name.stepin.es.store

import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import java.util.*

interface EventStoreReader {
    /**
     * Method for processors to find new events since particular event ID.
     * It can be interesting for incremental updates.
     *
     * There are filters by aggregator, aggregatorGuid, and event types.
     * Max number of new events can be specified.
     */
    fun <T : DomainEvent> findEventsSinceId(
        eventIdFrom: Long,
        aggregator: String? = null,
        aggregatorGuid: UUID? = null,
        accountGuid: AccountGuid? = null,
        eventTypes: List<String>? = null,
        maxBatchSize: Int? = null,
    ): Flow<DomainEventWithIdAndMeta<T>>

    /**
     * Method for processors to find new events since particular event GUID.
     * It can be interesting for incremental updates.
     *
     * There are filters by aggregator, aggregatorGuid, and event types.
     * Max number of new events can be specified.
     */
    fun <T : DomainEvent> findEventsSinceGuid(
        eventGuidFrom: UUID,
        aggregator: String? = null,
        aggregatorGuid: UUID? = null,
        accountGuid: AccountGuid? = null,
        eventTypes: List<String>? = null,
        maxBatchSize: Int? = null,
    ): Flow<DomainEventWithIdAndMeta<T>>

    /**
     * Method for processors to find new events since particular timestamp.
     * It can be interesting for incremental updates.
     *
     * There are filters by aggregator, aggregatorGuid, and event types.
     * Max number of new events can be specified.
     */
    fun <T : DomainEvent> findEventsSinceDate(
        date: LocalDateTime,
        aggregator: String? = null,
        aggregatorGuid: UUID? = null,
        accountGuid: AccountGuid? = null,
        eventTypes: List<String>? = null,
        maxBatchSize: Int? = null,
    ): Flow<DomainEventWithIdAndMeta<T>>

    /**
     * Method for processors to find all events.
     *
     * There are filters by aggregator, aggregatorGuid, and event types.
     * Max number of new events can be specified.
     */
    fun <T : DomainEvent> findEvents(
        aggregator: String? = null,
        aggregatorGuid: UUID? = null,
        accountGuid: AccountGuid? = null,
        eventTypes: List<String>? = null,
        maxBatchSize: Int? = null,
    ): Flow<DomainEventWithIdAndMeta<T>>
}
