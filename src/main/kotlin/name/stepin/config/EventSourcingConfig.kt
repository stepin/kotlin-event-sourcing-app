package name.stepin.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "es")
data class EventSourcingConfig(
    var startupType: StartupType = StartupType.NO_INITIAL_PROCESSING,
    var dbProcessingFrom: Long = 0,
    var dbProcessingTo: Long = -1,
    var eventsPackage: String = "",
    var processorsPackage: String = "",
)

enum class StartupType {
    SEED,
    DB_PROCESSING,
    NO_INITIAL_PROCESSING,
}
