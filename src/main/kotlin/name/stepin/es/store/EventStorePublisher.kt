package name.stepin.es.store

import java.util.*

interface EventStorePublisher {
    /**
     * Method waits for event processing to be finished.
     * Projector even can throw exception and event will be canceled.
     */
    suspend fun publish(
        events: List<DomainEventWithMeta>,
        skipReactor: Boolean = false,
    ): List<UUID>

    /**
     * Method waits for event processing to be finished.
     * Projector even can throw exception and event will be canceled.
     */
    suspend fun publish(
        event: DomainEvent,
        meta: EventMetadata = EventMetadata(),
        skipReactor: Boolean = false,
    ): UUID
}
