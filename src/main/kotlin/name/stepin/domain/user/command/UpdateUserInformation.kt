package name.stepin.domain.user.command

import name.stepin.db.repository.UserRepository
import name.stepin.domain.user.event.UserMetaUpdated
import name.stepin.es.store.EventStorePublisher
import name.stepin.exception.ErrorCode
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.stereotype.Service
import java.util.*

@Service
class UpdateUserInformation(
    private val store: EventStorePublisher,
    private val userRepository: UserRepository,
) {
    companion object : Logging

    data class Params(
        val userGuid: UUID,
        val firstName: String?,
        val secondName: String?,
        val displayName: String?,
    )

    suspend fun execute(params: Params): ErrorCode? =
        with(params) {
            val user =
                userRepository.findByGuid(userGuid)
                    ?: return ErrorCode.USER_NOT_FOUND

            val event =
                UserMetaUpdated(
                    accountGuid = user.accountGuid,
                    aggregatorGuid = user.guid,
                    firstName = if (firstName != user.firstName) firstName else null,
                    secondName = if (secondName != user.secondName) secondName else null,
                    displayName = if (displayName != user.displayName) displayName else null,
                )

            if (event.nothingChanged()) {
                logger.debug { "nothing changed, creation of event skipped $params" }
                return@with null
            }

            store.publish(event)
            return@with null
        }
}
