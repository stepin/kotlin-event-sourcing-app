package name.stepin.domain.user.email

import name.stepin.client.mail.ExternalMailService
import name.stepin.config.AppConfig
import org.springframework.stereotype.Service

@Service
class SendEmailService(
    private val service: ExternalMailService,
    private val appConfig: AppConfig,
) {
    suspend fun sendEmailConfirmationEmail(name: String, email: String, token: String) {
        val displayName = name.ifBlank { "Пользователь" }
        val subject = "Спасибо за регистрацию!"
        val text = """
               Здравствуйте, $displayName!

               Спасибо за регистрацию . Пожалуйста, перейдите по ссылке:

               ${appConfig.baseUrl}/welcome
        """.trimIndent()
        service.sendEmail(displayName, email, subject, text)
    }
}
