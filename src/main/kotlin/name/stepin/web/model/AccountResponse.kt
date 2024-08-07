package name.stepin.web.model

import name.stepin.db.entity.AccountEntity

class AccountResponse(
    val name: String,
) {
    companion object {
        fun from(entity: AccountEntity) =
            AccountResponse(
                name = entity.name,
            )
    }
}
