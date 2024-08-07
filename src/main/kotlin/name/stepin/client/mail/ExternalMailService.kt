package name.stepin.client.mail

import org.apache.logging.log4j.kotlin.Logging
import org.springframework.stereotype.Service

@Service
class ExternalMailService {
    companion object : Logging

    suspend fun sendEmail(
        displayName: String,
        email: String,
        subject: String,
        text: String,
    ) {
        logger.warn { "Just stub. $displayName $email $subject $text" }
    }
}
