package name.stepin.db.repository

import name.stepin.db.entity.AccountEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.*

interface AccountRepository : CoroutineCrudRepository<AccountEntity, Long> {
    suspend fun findByGuid(guid: UUID): AccountEntity?
}
