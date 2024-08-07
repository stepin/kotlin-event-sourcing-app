package name.stepin.fixture

import name.stepin.db.entity.AccountEntity
import java.time.Instant
import java.util.*

object AccountEntityFactory {
    fun accountEntity(id: Long = 1) =
        AccountEntity().apply {
            this.id = id
            guid = UUID.randomUUID()
            name = "name$id"
            createdAt = Instant.now()
        }
}
