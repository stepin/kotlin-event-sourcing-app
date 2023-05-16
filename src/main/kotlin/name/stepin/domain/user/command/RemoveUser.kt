package name.stepin.domain.user.command

import name.stepin.db.repository.UserRepository
import name.stepin.domain.user.event.UserRemoved
import name.stepin.es.store.EventStorePublisher
import name.stepin.exception.ErrorCode
import org.springframework.stereotype.Service
import java.util.*

@Service
class RemoveUser(
    private val store: EventStorePublisher,
    private val userRepository: UserRepository,
) {
    suspend fun execute(
        userGuid: UUID,
    ): ErrorCode? {
        val user = userRepository.findByGuid(userGuid)
            ?: return ErrorCode.USER_NOT_FOUND

        val event = UserRemoved(
            accountGuid = user.accountGuid,
            aggregatorGuid = user.guid,
        )

        store.publish(event)
        return null
    }
}
