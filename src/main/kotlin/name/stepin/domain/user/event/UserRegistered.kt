package name.stepin.domain.user.event

import name.stepin.es.store.AccountGuid
import name.stepin.es.store.EventGuid
import name.stepin.es.store.UserGuid
import java.util.*

data class UserRegistered(
    val email: String,
    val firstName: String?,
    val secondName: String?,
    val displayName: String,
    override val accountGuid: AccountGuid,
    override val aggregatorGuid: UserGuid = UUID.randomUUID(),
    override val guid: EventGuid = UUID.randomUUID(),
) : UserEvent(3)
