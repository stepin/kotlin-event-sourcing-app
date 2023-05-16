package name.stepin.db.dao

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import name.stepin.db.sql.tables.records.EventsRecord
import name.stepin.db.sql.tables.references.EVENTS
import name.stepin.es.store.AccountGuid
import name.stepin.fixture.PostgresFactory.dslContext
import name.stepin.fixture.PostgresFactory.initDb
import name.stepin.fixture.PostgresFactory.postgres
import org.jooq.DSLContext
import org.jooq.JSONB
import org.jooq.impl.DSL
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDateTime
import java.util.*

@Testcontainers
class EventDaoTest {

    @Container
    var postgres = postgres()

    private lateinit var db: DSLContext
    private lateinit var jdbcContext: DSLContext
    private lateinit var dao: EventDao
    private lateinit var firstGuid: UUID

    @BeforeEach
    fun setUp() {
        initDb(postgres)
        db = dslContext(postgres)
        jdbcContext = dslContext(postgres)

        dao = EventDao(db, jdbcContext)
        firstGuid = UUID.randomUUID()
        initDb()
    }

    private fun initDb() {
        db.delete(EVENTS).execute()
        createEvent(1)
        createEvent(2)
    }

    private fun createEvent(
        id: Long,
        accountGuid: AccountGuid = UUID.randomUUID(),
        aggregator: String = "user",
        aggregatorGuid: UUID = UUID.randomUUID(),
        type: String? = null,
    ) {
        val e = createEventRecord(id, accountGuid, aggregator, aggregatorGuid, type)
        e.store()
    }

    private fun createEventRecord(
        id: Long,
        accountGuid: AccountGuid = UUID.randomUUID(),
        aggregator: String = "user",
        aggregatorGuid: UUID = UUID.randomUUID(),
        type: String? = null,
    ): EventsRecord {
        val e = dao.newRecord()
        e.id = id
        e.guid = if (id == 1L) firstGuid else UUID.randomUUID()
        e.accountGuid = accountGuid
        e.aggregator = aggregator
        e.aggregatorGuid = aggregatorGuid
        e.type = type ?: "UserCreated$id"
        e.version = 0
        e.skip = false
        e.body = JSONB.valueOf("{\"user\": $id}")
        e.comment = "Just test record$id"
        e.createdAt = LocalDateTime.now()
        return e
    }

    @Test
    fun `newRecord main case`() {
        val e = createEventRecord(100)

        val actual = e.insert()

        assertEquals(1, actual)
        assertNotEquals(null, e.id)
        assertNotEquals(0, e.id)
    }

    @Test
    fun `byGuid not found case`() = runBlocking {
        val guid = UUID.randomUUID()

        val actual = dao.byGuid(guid)

        assertNull(actual)
    }

    @Test
    fun `byGuid found case`() = runBlocking {
        val actual = dao.byGuid(firstGuid)

        assertNotNull(actual)
        assertEquals(firstGuid, actual?.guid)
        assertEquals("user", actual?.aggregator)
    }

    @Test
    fun `isNoEvents true case`() = runBlocking {
        db.delete(EVENTS).execute()

        val actual = dao.isNoEvents()

        assertTrue(actual)
    }

    @Test
    fun `isNoEvents false case`() = runBlocking {
        val actual = dao.isNoEvents()

        assertFalse(actual)
    }

    @Test
    fun `getEventsListOnCondition empty case`() = runBlocking {
        val actual = dao.getEventsListOnCondition(
            mainCondition = DSL.noCondition(),
            accountGuid = null,
            aggregator = null,
            aggregatorGuid = null,
            eventTypes = null,
            maxBatchSize = null,
        )

        val actualList = actual.toList()
        assertEquals(2, actualList.size)
    }

    @Test
    fun `getEventsListOnCondition emptyList case`() = runBlocking {
        val actual = dao.getEventsListOnCondition(
            mainCondition = DSL.noCondition(),
            accountGuid = null,
            aggregator = null,
            aggregatorGuid = null,
            eventTypes = emptyList(),
            maxBatchSize = null,
        )

        val actualList = actual.toList()
        assertEquals(2, actualList.size)
    }

    @Test
    fun `getEventsListOnCondition main case`() = runBlocking {
        val accountGuid = UUID.randomUUID()
        val accountGuid2 = UUID.randomUUID()
        val aggregatorGuid = UUID.randomUUID()
        val aggregatorGuid2 = UUID.randomUUID()
        val evenTypes = listOf("accountCreated", "accountDeleted")
        createEvent(20, accountGuid, "account", aggregatorGuid, "accountCreated")
        createEvent(21, accountGuid, "account", aggregatorGuid, "accountUpdated")
        createEvent(22, accountGuid, "account", aggregatorGuid, "accountDeleted")
        createEvent(23, accountGuid, "account", aggregatorGuid, "accountDeleted")
        createEvent(24, accountGuid, "account", aggregatorGuid, "accountDeleted")
        createEvent(25, accountGuid, "account", aggregatorGuid, "accountDeleted")
        createEvent(30, accountGuid2, "account", aggregatorGuid, "accountCreated")
        createEvent(31, accountGuid, "account", aggregatorGuid2, "accountCreated")

        val actual = dao.getEventsListOnCondition(
            mainCondition = DSL.noCondition(),
            accountGuid = accountGuid,
            aggregator = "account",
            aggregatorGuid = aggregatorGuid,
            eventTypes = evenTypes,
            maxBatchSize = null,
        )

        val actualList = actual.toList()
        assertEquals(listOf(20L, 22, 23, 24, 25), actualList.map { it.value1().id })
    }

    @Test
    fun `getEventsListOnCondition main with main condition and batch case`() = runBlocking {
        val accountGuid = UUID.randomUUID()
        val accountGuid2 = UUID.randomUUID()
        val aggregatorGuid = UUID.randomUUID()
        val aggregatorGuid2 = UUID.randomUUID()
        val evenTypes = listOf("accountCreated", "accountDeleted")
        createEvent(20, accountGuid, "account", aggregatorGuid, "accountCreated")
        createEvent(21, accountGuid, "account", aggregatorGuid, "accountUpdated")
        createEvent(22, accountGuid, "account", aggregatorGuid, "accountDeleted")
        createEvent(23, accountGuid, "account", aggregatorGuid, "accountDeleted")
        createEvent(24, accountGuid, "account", aggregatorGuid, "accountDeleted")
        createEvent(25, accountGuid, "account", aggregatorGuid, "accountDeleted")
        createEvent(30, accountGuid2, "account", aggregatorGuid, "accountCreated")
        createEvent(31, accountGuid, "account", aggregatorGuid2, "accountCreated")

        val actual = dao.getEventsListOnCondition(
            mainCondition = EVENTS.ID.gt(20),
            accountGuid = accountGuid,
            aggregator = "account",
            aggregatorGuid = aggregatorGuid,
            eventTypes = evenTypes,
            maxBatchSize = 3,
        )

        val actualList = actual.toList()
        assertEquals(listOf(22L, 23, 24), actualList.map { it.value1().id })
    }
}
