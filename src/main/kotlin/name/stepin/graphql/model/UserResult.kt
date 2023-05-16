package name.stepin.graphql.model

import name.stepin.db.entity.UserEntity

data class UserResult(
    val name: String,
    val email: String,
) {

    companion object {
        fun from(entity: UserEntity) = UserResult(
            name = entity.displayName ?: "<none>",
            email = entity.email,
        )
    }
}
