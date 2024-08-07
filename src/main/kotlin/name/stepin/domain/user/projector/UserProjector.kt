package name.stepin.domain.user.projector

import name.stepin.db.entity.UserEntity
import name.stepin.db.repository.AccountRepository
import name.stepin.db.repository.UserRepository
import name.stepin.domain.user.event.UserMetaUpdated
import name.stepin.domain.user.event.UserRegistered
import name.stepin.domain.user.event.UserRemoved
import name.stepin.es.handler.Projector
import name.stepin.es.store.EventMetadata
import name.stepin.exception.DomainException
import name.stepin.exception.ErrorCode
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.stereotype.Service
import java.time.ZoneOffset
import java.util.*

@Service
class UserProjector(
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository,
) {
    companion object : Logging

    @Projector
    suspend fun handleUserMetaUpdated(e: UserMetaUpdated) {
        val user = getUser(e.aggregatorGuid)

        if (e.firstName != null) {
            user.firstName = e.firstName
        }
        if (e.secondName != null) {
            user.secondName = e.secondName
        }
        if (e.displayName != null) {
            user.displayName = e.displayName
        }

        userRepository.save(user)
    }

    @Projector
    suspend fun handleUserRegistered(
        e: UserRegistered,
        meta: EventMetadata,
    ) {
        val account = accountRepository.findByGuid(e.accountGuid)

        val u = UserEntity()
        u.accountGuid = e.accountGuid
        u.accountId = account?.id ?: 0
        u.guid = e.aggregatorGuid
        u.email = e.email
        u.displayName = e.displayName
        u.firstName = e.firstName
        u.secondName = e.secondName
        u.createdAt = meta.createdAt.toInstant(ZoneOffset.UTC)

        val savedUser = userRepository.save(u)
        logger.debug { "new user id: ${savedUser.id}" }
    }

    @Projector
    suspend fun handleUserRemoved(
        e: UserRemoved,
        meta: EventMetadata,
    ) {
        val user = getUser(e.aggregatorGuid)

        userRepository.delete(user)
    }

    private suspend fun getUser(userGuid: UUID) =
        userRepository.findByGuid(userGuid)
            ?: throw DomainException(ErrorCode.USER_NOT_FOUND)
}
