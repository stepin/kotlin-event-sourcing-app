package name.stepin.domain.account.event

import name.stepin.es.store.AccountGuid
import name.stepin.es.store.DomainEvent
import java.util.*

sealed class AccountEvent(
    override val eventTypeVersion: Short = 0,
) : DomainEvent {
    override val aggregatorType: String
        get() = "account"

    override val eventType: String
        get() = this.javaClass.simpleName

    override val aggregatorGuid: AccountGuid
        get() = accountGuid
}
