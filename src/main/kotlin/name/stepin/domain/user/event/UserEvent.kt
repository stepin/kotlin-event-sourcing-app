package name.stepin.domain.user.event

import name.stepin.es.store.DomainEvent
import name.stepin.es.store.UserGuid
import java.util.*

sealed class UserEvent(
    override val eventTypeVersion: Short = 0,
) : DomainEvent {
    override val aggregatorType: String
        get() = "user"

    override val eventType: String
        get() = this.javaClass.simpleName

    abstract override val aggregatorGuid: UserGuid
}
