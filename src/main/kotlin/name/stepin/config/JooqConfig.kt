package name.stepin.config

import io.r2dbc.spi.ConnectionFactory
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class JooqConfig {
    @Primary
    @Bean
    fun dslContext(connectionFactory: ConnectionFactory) = DSL.using(connectionFactory, SQLDialect.POSTGRES)
}
