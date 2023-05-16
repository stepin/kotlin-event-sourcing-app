package name.stepin.domain.account.event

import name.stepin.es.store.AccountGuid
import name.stepin.es.store.EventGuid
import java.util.*

data class AccountCreated(
    val name: String,
    val userGuid: UUID,
    override val accountGuid: AccountGuid = UUID.randomUUID(),
    override val guid: EventGuid = UUID.randomUUID(),
) : AccountEvent()
