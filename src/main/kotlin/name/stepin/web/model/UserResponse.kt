package name.stepin.web.model

import name.stepin.db.entity.UserEntity

data class UserResponse(
    val name: String,
    val email: String,
) {
    companion object {
        fun from(entity: UserEntity) = UserResponse(
            name = entity.displayName ?: "<none>",
            email = entity.email,
        )
    }
}
