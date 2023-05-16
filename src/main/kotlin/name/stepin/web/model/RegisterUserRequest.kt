package name.stepin.web.model

import name.stepin.domain.user.command.RegisterUser

data class RegisterUserRequest(
    val email: String,
    val firstName: String?,
    val secondName: String?,
    val displayName: String?,
) {
    fun toCommandParams() = RegisterUser.Params(
        email = email,
        firstName = firstName,
        secondName = secondName,
        displayName = displayName,
    )
}
