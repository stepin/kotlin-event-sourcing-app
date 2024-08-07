package name.stepin.web

import name.stepin.domain.user.command.RegisterUser
import name.stepin.domain.user.command.RemoveUser
import name.stepin.domain.user.command.UpdateUserInformation
import name.stepin.web.model.RegisterUserRequest
import name.stepin.web.model.UpdateUserRequest
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
class AppController(
    private val registerUser: RegisterUser,
    private val updateUserInformation: UpdateUserInformation,
    private val removeUser: RemoveUser,
) {
    @PostMapping("/api/users")
    suspend fun registerUser(
        @RequestBody registerUserRequest: RegisterUserRequest,
    ): UUID {
        when (val result = registerUser.execute(registerUserRequest.toCommandParams())) {
            is RegisterUser.Response.Created -> return result.userGuid
            is RegisterUser.Response.Error -> throw RuntimeException(result.errorCode.toString())
        }
    }

    @PostMapping("/api/users/{guid}")
    suspend fun updateUser(
        @PathVariable guid: UUID,
        @RequestBody updateUserRequest: UpdateUserRequest,
    ) {
        updateUserInformation.execute(updateUserRequest.toCommandParams(guid))
    }

    @DeleteMapping("/api/users/{guid}")
    suspend fun deleteUser(
        @PathVariable guid: UUID,
    ) {
        removeUser.execute(guid)
    }
}
