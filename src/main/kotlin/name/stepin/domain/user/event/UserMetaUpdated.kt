package name.stepin.domain.user.event

import name.stepin.es.store.AccountGuid
import name.stepin.es.store.EventGuid
import name.stepin.es.store.UserGuid
import java.util.*

data class UserMetaUpdated(
    val firstName: String?,
    val secondName: String?,
    val displayName: String?,
    override val aggregatorGuid: UserGuid,
    override val accountGuid: AccountGuid,
    override val guid: EventGuid = UUID.randomUUID(),
) : UserEvent() {
    fun nothingChanged(): Boolean = firstName == null && secondName == null && displayName == null
}
