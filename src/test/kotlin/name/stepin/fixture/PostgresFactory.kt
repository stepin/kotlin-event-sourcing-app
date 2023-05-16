package name.stepin.fixture

import name.stepin.es.TransactionIntegrationTests
import org.flywaydb.core.Flyway
import org.jooq.impl.DSL
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.PostgreSQLContainer

object PostgresFactory {

    fun postgres(): PostgreSQLContainer<*> = PostgreSQLContainer("postgres:15.2")
        .withReuse(true)

    fun dslContext(postgres: PostgreSQLContainer<*>) = DSL.using(postgres.createConnection(""))

    fun dataSource(postgres: PostgreSQLContainer<*>): PGSimpleDataSource =
        PGSimpleDataSource().apply {
            setUrl(postgres.jdbcUrl)
            user = postgres.username
            password = postgres.password
        }

    fun initDb(postgres: PostgreSQLContainer<*>) {
        while (!TransactionIntegrationTests.postgresContainer.isRunning) {
            Thread.sleep(100)
        }

        val flyway = Flyway.configure()
            .schemas("public")
            .dataSource(dataSource(postgres))
            .load()

        flyway.migrate()
    }

    fun postgresProperties(registry: DynamicPropertyRegistry) {
        registry.add("spring.datasource.url") { TransactionIntegrationTests.postgresContainer.jdbcUrl }
        registry.add("spring.datasource.username") { TransactionIntegrationTests.postgresContainer.username }
        registry.add("spring.datasource.password") { TransactionIntegrationTests.postgresContainer.password }
        registry.add("spring.r2dbc.url") {
            "r2dbc:postgresql://" +
                "${TransactionIntegrationTests.postgresContainer.host}:${TransactionIntegrationTests.postgresContainer.firstMappedPort}/${TransactionIntegrationTests.postgresContainer.databaseName}"
        }
        registry.add("spring.r2dbc.username") { TransactionIntegrationTests.postgresContainer.username }
        registry.add("spring.r2dbc.password") { TransactionIntegrationTests.postgresContainer.password }
    }
}
