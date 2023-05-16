package name.stepin.es.processor

import name.stepin.db.dao.EventDao
import name.stepin.domain.account.event.AccountCreated
import name.stepin.domain.user.event.UserMetaUpdated
import name.stepin.domain.user.event.UserRegistered
import name.stepin.es.store.DomainEventWithMeta
import name.stepin.es.store.EventMetadata
import name.stepin.es.store.EventStorePublisher
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class Seed(
    private val eventStorePublisher: EventStorePublisher,
    private val eventDao: EventDao,
) {
    companion object : Logging

    @Transactional
    suspend fun run(): Boolean {
        val start = System.currentTimeMillis()
        val execute = eventDao.isNoEvents()
        if (execute) {
            val events = events()
            eventStorePublisher.publish(events, skipReactor = true)
        }
        val duration = System.currentTimeMillis() - start
        logger.info { "Seed finished in ${duration}ms" }
        return execute
    }

    private fun events(): List<DomainEventWithMeta> {
        val r = mutableListOf<DomainEventWithMeta>()
        val meta = EventMetadata()

        val accountGuid = UUID.randomUUID()
        val userGuid = UUID.randomUUID()

        val userRegistered = UserRegistered(
            accountGuid = accountGuid,
            aggregatorGuid = userGuid,
            email = "test@example.com",
            displayName = "Иваныч",
            firstName = "Иван",
            secondName = "Иванов",
        )
        r.add(userRegistered to meta)

        val accountCreated = AccountCreated(
            name = "test",
            accountGuid = accountGuid,
            userGuid = userGuid,
        )
        r.add(accountCreated to meta)

        val nameChanged = UserMetaUpdated(
            accountGuid = accountGuid,
            aggregatorGuid = userGuid,
            firstName = "firstName2",
            secondName = null,
            displayName = null,
        )
        r.add(nameChanged to meta)

        return r
    }
}
