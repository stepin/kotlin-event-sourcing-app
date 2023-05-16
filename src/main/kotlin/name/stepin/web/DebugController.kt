package name.stepin.web

import io.swagger.v3.oas.annotations.Operation
import name.stepin.service.DebugService
import name.stepin.web.model.AccountResponse
import name.stepin.web.model.UserResponse
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@Validated
class DebugController(
    private val debugService: DebugService,
) {
    companion object : Logging

    @GetMapping("/api/debug/users")
    @Operation(summary = "All users for debug (it's only an example)")
    suspend fun allUsers() = debugService.getUsers().map { UserResponse.from(it) }

    @GetMapping("/api/debug/accounts")
    @Operation(summary = "All accounts for debug (it's only an example)")
    suspend fun allAccounts() = debugService.getAccounts().map { AccountResponse.from(it) }
}
