package name.stepin.domain.user.event

import name.stepin.es.store.AccountGuid
import name.stepin.es.store.EventGuid
import name.stepin.es.store.UserGuid
import java.util.*

data class UserRemoved(
    override val aggregatorGuid: UserGuid,
    override val accountGuid: AccountGuid,
    override val guid: EventGuid = UUID.randomUUID(),
) : UserEvent()
