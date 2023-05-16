package name.stepin.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "app")
data class AppConfig(
    var baseUrl: String = "",
)
