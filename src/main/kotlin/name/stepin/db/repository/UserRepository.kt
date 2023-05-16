package name.stepin.db.repository

import kotlinx.coroutines.flow.Flow
import name.stepin.db.entity.UserEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.time.Instant
import java.util.UUID

interface UserRepository : CoroutineCrudRepository<UserEntity, Long> {
    suspend fun findByGuid(guid: UUID): UserEntity?
    suspend fun findByEmail(email: String): UserEntity?
    fun findAllByCreatedAtAfter(date: Instant): Flow<UserEntity>
}
