package name.stepin.web.model

import name.stepin.domain.user.command.UpdateUserInformation
import java.util.*

data class UpdateUserRequest(
    val firstName: String?,
    val secondName: String?,
    val displayName: String?,
) {
    fun toCommandParams(userGuid: UUID) =
        UpdateUserInformation.Params(
            userGuid = userGuid,
            firstName = firstName,
            secondName = secondName,
            displayName = displayName,
        )
}
