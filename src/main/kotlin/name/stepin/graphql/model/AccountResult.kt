package name.stepin.graphql.model

import name.stepin.db.entity.AccountEntity

class AccountResult(
    val name: String,
) {
    companion object {
        fun from(entity: AccountEntity) = AccountResult(
            name = entity.name,
        )
    }
}
