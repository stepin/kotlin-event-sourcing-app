package name.stepin.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import name.stepin.db.entity.AccountEntity
import name.stepin.db.entity.UserEntity
import name.stepin.db.repository.AccountRepository
import name.stepin.db.repository.UserRepository
import name.stepin.domain.user.event.UserEvent
import name.stepin.domain.user.event.UserMetaUpdated
import name.stepin.domain.user.event.UserRegistered
import name.stepin.domain.user.event.UserRemoved
import name.stepin.es.store.EventStoreReader
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

@Service
class DebugService(
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository,
    private val eventStoreReader: EventStoreReader,
) {

    suspend fun getUsers(): List<UserEntity> {
        return userRepository.findAll().toList()
    }

    suspend fun getUsersSince(date: LocalDateTime): List<UserEntity> {
        return userRepository.findAllByCreatedAtAfter(date.toInstant(ZoneOffset.UTC)).toList()
    }

    fun getUserAudit(userGuid: UUID): Flow<String> {
        return eventStoreReader.findEvents<UserEvent>("user", userGuid, maxBatchSize = 100)
            .map { (id, e, meta) ->
                when (e) {
                    is UserMetaUpdated -> "updated $e"
                    is UserRegistered -> "user registered with id $id ${meta.createdAt} $e"
                    is UserRemoved -> "user deleted at ${meta.createdAt}"
                }
            }
    }

    suspend fun getAccounts(): List<AccountEntity> {
        return accountRepository.findAll().toList()
    }
}
