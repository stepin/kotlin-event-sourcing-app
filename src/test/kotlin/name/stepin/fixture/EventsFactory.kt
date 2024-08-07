package name.stepin.fixture

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import name.stepin.domain.user.event.UserEvent
import name.stepin.domain.user.event.UserMetaUpdated
import name.stepin.domain.user.event.UserRegistered
import name.stepin.domain.user.event.UserRemoved
import name.stepin.es.store.DomainEventWithIdAndMeta
import name.stepin.es.store.EventMetadata
import java.time.LocalDateTime
import java.util.*

object EventsFactory {
    fun userRegistered(id: Int) =
        UserRegistered(
            email = "email$id@example.com",
            firstName = "firstName$id",
            secondName = "secondName$id",
            displayName = "displayName$id",
            accountGuid = UUID.randomUUID(),
            aggregatorGuid = UUID.randomUUID(),
            guid = UUID.randomUUID(),
        )

    fun userMetaUpdated(id: Int) =
        UserMetaUpdated(
            firstName = "firstName$id",
            secondName = "secondName$id",
            displayName = "displayName$id",
            accountGuid = UUID.randomUUID(),
            aggregatorGuid = UUID.randomUUID(),
            guid = UUID.randomUUID(),
        )

    fun flow3events(
        accountGuid: UUID = UUID.randomUUID(),
        userGuid: UUID = UUID.randomUUID(),
        eventGuid1: UUID = UUID.randomUUID(),
        eventGuid2: UUID = UUID.randomUUID(),
    ): Flow<DomainEventWithIdAndMeta<UserEvent>> {
        val createdAt1 = LocalDateTime.of(2023, 1, 1, 1, 1)
        val createdAt2 = LocalDateTime.of(2023, 1, 2, 1, 1)
        val createdAt3 = LocalDateTime.of(2023, 1, 3, 1, 1)
        val userRegistered =
            UserRegistered(
                email = "cara.rivas@example.com",
                firstName = null,
                secondName = null,
                displayName = "Peggy Fuller",
                accountGuid = accountGuid,
                aggregatorGuid = userGuid,
                guid = eventGuid1,
            )
        val userMetaUpdated =
            UserMetaUpdated(
                firstName = "firstName2",
                secondName = null,
                displayName = null,
                aggregatorGuid = userGuid,
                accountGuid = accountGuid,
                guid = eventGuid2,
            )
        val userRemoved =
            UserRemoved(
                aggregatorGuid = userGuid,
                accountGuid = accountGuid,
            )
        return flowOf(
            DomainEventWithIdAndMeta(1, userRegistered, EventMetadata(createdAt = createdAt1)),
            DomainEventWithIdAndMeta(2, userMetaUpdated, EventMetadata(createdAt = createdAt2)),
            DomainEventWithIdAndMeta(3, userRemoved, EventMetadata(createdAt = createdAt3)),
        )
    }
}
