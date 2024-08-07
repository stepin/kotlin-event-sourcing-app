package name.stepin.es.processor

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import name.stepin.db.dao.EventDao
import name.stepin.es.store.DomainEventWithMeta
import name.stepin.es.store.EventStorePublisher
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SeedTest {
    private lateinit var service: Seed

    @MockK
    lateinit var eventStorePublisher: EventStorePublisher

    @MockK
    lateinit var eventDao: EventDao

    @BeforeEach
    fun setUp() {
        service = Seed(eventStorePublisher, eventDao)
    }

    @AfterEach
    fun tearDown() {
        confirmVerified(eventStorePublisher, eventDao)
    }

    @Test
    fun `events already exists`() =
        runBlocking {
            coEvery { eventDao.isNoEvents() } returns false

            val actual = service.run()

            assertFalse(actual)
            coVerify(exactly = 1) { eventDao.isNoEvents() }
        }

    @Test
    fun `success case`() =
        runBlocking {
            coEvery { eventDao.isNoEvents() } returns true
            coEvery {
                eventStorePublisher.publish(any<List<DomainEventWithMeta>>(), skipReactor = true)
            } returns emptyList()

            val actual = service.run()

            assertTrue(actual)
            coVerify(exactly = 1) { eventDao.isNoEvents() }
            coVerify(exactly = 1) {
                eventStorePublisher.publish(any<List<DomainEventWithMeta>>(), skipReactor = true)
            }
        }
}
