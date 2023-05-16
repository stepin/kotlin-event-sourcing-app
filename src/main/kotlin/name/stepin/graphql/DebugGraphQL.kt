package name.stepin.graphql

import kotlinx.coroutines.flow.toList
import name.stepin.graphql.model.AccountResult
import name.stepin.graphql.model.UserResult
import name.stepin.service.DebugService
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller
import org.springframework.validation.annotation.Validated
import java.time.LocalDateTime
import java.util.*

@Controller
@Validated
class DebugGraphQL(
    private val debugService: DebugService,
) {
    companion object : Logging

    @QueryMapping
    suspend fun allUsers(): List<UserResult> {
        logger.info("allUsers")
        return debugService.getUsers().map { UserResult.from(it) }
    }

    @QueryMapping
    suspend fun usersSince(@Argument date: LocalDateTime): List<UserResult> {
        logger.info("usersSince")
        return debugService.getUsersSince(date).map { UserResult.from(it) }
    }

    @QueryMapping
    suspend fun userAudit(@Argument userGuid: UUID): List<String> {
        logger.info("userAudit")
        // NOTE: flow is not supported by Spring Boot GraphQL
        return debugService.getUserAudit(userGuid).toList()
    }

    @QueryMapping
    suspend fun allAccounts(): List<AccountResult> {
        logger.info("allAccounts")
        return debugService.getAccounts().map { AccountResult.from(it) }
    }
}
