package name.stepin.db.dao

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import name.stepin.db.sql.tables.records.EventsRecord
import name.stepin.db.sql.tables.references.EVENTS
import name.stepin.es.store.AccountGuid
import name.stepin.es.store.EventGuid
import org.apache.logging.log4j.kotlin.Logging
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.impl.DSL.noCondition
import org.springframework.stereotype.Service
import java.util.*

@Service
class EventDao(
    private val db: DSLContext,
) {
    companion object : Logging

    suspend fun byGuid(guid: EventGuid): EventsRecord? = db
        .select(EVENTS)
        .from(EVENTS)
        .where(EVENTS.GUID.eq(guid))
        .awaitFirstOrNull()
        ?.value1()

    suspend fun eventsCount(): Int = db
        .selectCount()
        .from(EVENTS)
        .awaitFirst()
        .value1()

    suspend fun isNoEvents(): Boolean = eventsCount() == 0

    fun getEventsListOnCondition(
        mainCondition: Condition,
        accountGuid: AccountGuid?,
        aggregator: String?,
        aggregatorGuid: UUID?,
        eventTypes: List<String>?,
        maxBatchSize: Int?,
    ): Flow<Record1<EventsRecord>> {
        val queryWithoutLimit = db
            .select(EVENTS)
            .from(EVENTS)
            .where(mainCondition)
            .and(
                if (accountGuid != null) {
                    EVENTS.ACCOUNT_GUID.eq(accountGuid)
                } else {
                    noCondition()
                },
            )
            .and(
                if (aggregator != null) {
                    EVENTS.AGGREGATOR.eq(aggregator)
                } else {
                    noCondition()
                },
            )
            .and(
                if (aggregatorGuid != null) {
                    EVENTS.AGGREGATOR_GUID.eq(aggregatorGuid)
                } else {
                    noCondition()
                },
            )
            .and(
                if (!eventTypes.isNullOrEmpty()) {
                    EVENTS.TYPE.`in`(eventTypes)
                } else {
                    noCondition()
                },
            )
            .orderBy(EVENTS.ID.asc())
        val query = if (maxBatchSize != null) queryWithoutLimit.limit(maxBatchSize) else queryWithoutLimit

        return query.asFlow()
    }
}
