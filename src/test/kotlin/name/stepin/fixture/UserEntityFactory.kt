package name.stepin.fixture

import name.stepin.db.entity.UserEntity
import java.time.Instant
import java.util.*

object UserEntityFactory {
    fun userEntity(id: Long = 1) =
        UserEntity().apply {
            this.id = id
            guid = UUID.randomUUID()
            accountId = 1000 + id
            accountGuid = UUID.randomUUID()
            displayName = "displayName$id"
            firstName = "firstName$id"
            secondName = "lastName$id"
            email = "me$id@example.com"
            createdAt = Instant.now()
        }
}
