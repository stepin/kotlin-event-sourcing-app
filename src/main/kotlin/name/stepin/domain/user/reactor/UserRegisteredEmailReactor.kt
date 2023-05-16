package name.stepin.domain.user.reactor

import name.stepin.domain.user.email.SendEmailService
import name.stepin.domain.user.event.UserRegistered
import name.stepin.es.handler.Reactor
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.stereotype.Service

@Service
class UserRegisteredEmailReactor(
    private val emailService: SendEmailService,
) {
    companion object : Logging

    @Reactor
    suspend fun handle(e: UserRegistered) {
        emailService.sendEmailConfirmationEmail(e.displayName, e.email, e.aggregatorGuid.toString())
    }
}
