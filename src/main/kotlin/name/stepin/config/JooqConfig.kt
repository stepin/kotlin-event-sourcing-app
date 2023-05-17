package name.stepin.config

import com.zaxxer.hikari.HikariDataSource
import org.jooq.*
import org.jooq.impl.DSL
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class JooqConfig {

    @Primary
    @Bean
    fun dslContext(r2dbcConfig: R2dbcConfig) = DSL.using(r2dbcConfig.connectionFactory(), SQLDialect.POSTGRES)

    @Bean("jdbcDb")
    fun jdbcDslContext(dataSource: HikariDataSource): DSLContext = DSL.using(dataSource, SQLDialect.POSTGRES)
}
