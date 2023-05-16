package name.stepin.es.processor

import kotlinx.coroutines.runBlocking
import name.stepin.config.EventSourcingConfig
import name.stepin.config.StartupType
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicBoolean

@Profile("!test")
@Component
class EventSourcingStarter(
    private val config: EventSourcingConfig,
    private val seed: Seed,
    private val inlineProcessor: InlineProcessor,
) : ApplicationRunner, HealthIndicator {

    companion object : Logging {
        private val initialized = AtomicBoolean(false)
    }

    override fun run(args: ApplicationArguments) {
        when (config.startupType) {
            StartupType.SEED -> {
                logger.info("seed started")
                runBlocking {
                    seed.run()
                }
                logger.info("seed finished")
            }

            StartupType.DB_PROCESSING -> {
                runBlocking {
                    logger.info("initial events' processing started")
                    inlineProcessor.replayEvents(
                        from = config.dbProcessingFrom,
                        to = config.dbProcessingTo,
                        skipReactor = true,
                    )
                    logger.info("initial events' processing finished")
                    // we don't need side effects here, only to re-create DB
                }
            }

            StartupType.NO_INITIAL_PROCESSING -> {
                // nothing to do
            }
        }

        initialized.set(true)
    }

    override fun health(): Health {
        return if (initialized.get()) {
            Health.up().build()
        } else {
            Health.down().build()
        }
    }

    fun reset() {
        initialized.set(false)
    }
}
