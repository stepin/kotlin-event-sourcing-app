package name.stepin.es.store

import java.time.LocalDateTime
import java.util.*

/**
 * When event is not specific to any account. Or system user.
 */
val SYSTEM_GUID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")

typealias EventGuid = UUID
typealias AccountGuid = UUID
typealias UserGuid = UUID

interface DomainEvent {
    /**
     * To be able to partition DB by accounts.
     * System-wide event has 0s in UUID value.
     */
    val accountGuid: AccountGuid

    /**
     * Uniq event ID (in most cases it's Guid for new objects also).
     */
    val guid: EventGuid

    /**
     * Event is created on Aggregator (type and id).
     */
    val aggregatorType: String

    /**
     * Event is created on Aggregator (type and id).
     */
    val aggregatorGuid: UUID

    /**
     * Even name inside Aggregator.
     */
    val eventType: String

    /**
     * Even version inside Aggregator. Overtime new versions of the same evenType can be introduced.
     */
    val eventTypeVersion: Short
}

data class EventMetadata(
    /**
     * Person that created event.
     * Null means that event were created by the system (like timer).
     */
    val creatorGuid: UserGuid = SYSTEM_GUID,

    /**
     * Timezone is UTC.
     */
    val createdAt: LocalDateTime = LocalDateTime.now(),

    /**
     * Any human-readable text.
     */
    val comment: String = "",

    /**
     * Skip event for technical reason.
     */
    val skip: Boolean = false,
)

typealias DomainEventWithMeta = Pair<DomainEvent, EventMetadata>

data class DomainEventWithIdAndMeta<out T : DomainEvent>(
    val id: Long,
    val event: T,
    val meta: EventMetadata,
)
