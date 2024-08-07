package name.stepin.domain.account.projector

import name.stepin.db.entity.AccountEntity
import name.stepin.db.repository.AccountRepository
import name.stepin.db.repository.UserRepository
import name.stepin.domain.account.event.AccountCreated
import name.stepin.es.handler.Projector
import name.stepin.es.store.EventMetadata
import name.stepin.exception.DomainException
import name.stepin.exception.ErrorCode
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.stereotype.Service
import java.time.ZoneOffset

@Service
class AccountCreatedProjector(
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository,
) {
    companion object : Logging

    @Projector
    suspend fun handle(
        e: AccountCreated,
        meta: EventMetadata,
    ) {
        val user =
            userRepository.findByGuid(e.userGuid)
                ?: throw DomainException(ErrorCode.USER_NOT_FOUND)

        val a = AccountEntity()
        a.guid = e.aggregatorGuid
        a.name = e.name
        a.userId = user.id!!
        a.createdAt = meta.createdAt.toInstant(ZoneOffset.UTC)

        val savedAccount = accountRepository.save(a)

        user.accountId = savedAccount.id!!
        userRepository.save(user)

        logger.debug { "new account id: ${savedAccount.id}" }
    }
}
