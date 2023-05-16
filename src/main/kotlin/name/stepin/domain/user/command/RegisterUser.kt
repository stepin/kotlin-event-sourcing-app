package name.stepin.domain.user.command

import name.stepin.db.repository.UserRepository
import name.stepin.domain.account.event.AccountCreated
import name.stepin.domain.user.event.UserRegistered
import name.stepin.domain.user.model.calcDisplayName
import name.stepin.es.store.EventStorePublisher
import name.stepin.exception.ErrorCode
import org.springframework.stereotype.Service
import java.util.*

@Service
class RegisterUser(
    private val store: EventStorePublisher,
    private val userRepository: UserRepository,
) {

    data class Params(
        val email: String,
        val firstName: String?,
        val secondName: String?,
        val displayName: String?,
    )

    sealed class Response {
        data class Created(val userGuid: UUID) : Response()
        data class Error(val errorCode: ErrorCode) : Response()
    }

    suspend fun execute(params: Params): Response = with(params) {
        val user = userRepository.findByEmail(email)
        if (user != null) {
            return Response.Error(ErrorCode.USER_ALREADY_REGISTERED)
        }

        val accountGuid = UUID.randomUUID()
        val userGuid = UUID.randomUUID()

        val userRegistered = UserRegistered(
            accountGuid = accountGuid,
            aggregatorGuid = userGuid,
            email = email,
            firstName = firstName,
            secondName = secondName,
            displayName = displayName ?: calcDisplayName(email, firstName, secondName),
        )
        store.publish(userRegistered)

        val accountCreated = AccountCreated(
            name = "Неизвестная компания",
            accountGuid = accountGuid,
            userGuid = userGuid,
        )
        store.publish(accountCreated)

        return Response.Created(userGuid)
    }
}
