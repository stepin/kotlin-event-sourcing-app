package name.stepin.fixture

import org.rnorth.ducttape.unreliables.Unreliables
import org.testcontainers.containers.ContainerLaunchException
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.WaitStrategy
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget
import org.testcontainers.exception.ConnectionCreationException
import java.sql.Connection
import java.sql.DriverManager
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit

class PostgresWaitingStrategy(
    private var startupTimeout: Duration = Duration.ofSeconds(60),
) : WaitStrategy {

    override fun waitUntilReady(waitStrategyTarget: WaitStrategyTarget) {
        try {
            Unreliables.retryUntilSuccess(startupTimeout.seconds.toInt(), TimeUnit.SECONDS) {
                try {
                    val connection = getConnection(waitStrategyTarget)
                    return@retryUntilSuccess executeSql(connection)
                } catch (e: RuntimeException) {
                    return@retryUntilSuccess false
                }
            }
            // NOTE: it's unclear why successful SQL call new connections still can't be created for some time
            Thread.sleep(5000)
        } catch (e: Exception) {
            throw ContainerLaunchException("Startup timeout")
        }
    }

    private fun getConnection(waitStrategyTarget: WaitStrategyTarget): Connection {
        val container = waitStrategyTarget as PostgreSQLContainer<*>
        try {
            val connectionProps = Properties().apply {
                put("user", container.username)
                put("password", container.password)
            }
            return DriverManager.getConnection(container.jdbcUrl, connectionProps)
        } catch (e: Exception) {
            throw ConnectionCreationException("Could not obtain PostgresSQL connection", e)
        }
    }

    private fun executeSql(connection: Connection): Boolean {
        return try {
            val result = connection.prepareStatement("SELECT 123").executeQuery()
            result.next()
            val value = result.getObject(1, Integer::class.java).toInt()
            value == 123
        } catch (e: Exception) {
            false
        }
    }

    override fun withStartupTimeout(startupTimeout: Duration): WaitStrategy {
        this.startupTimeout = startupTimeout
        return this
    }
}
