package name.stepin.es.handler

import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import name.stepin.config.EventSourcingConfig
import name.stepin.domain.user.event.UserRegistered
import name.stepin.domain.user.reactor.UserRegisteredEmailReactor
import name.stepin.es.store.EventMetadata
import name.stepin.fixture.EventsFactory.userRegistered
import org.apache.logging.log4j.kotlin.Logging
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.context.ApplicationContext

@ExtendWith(MockKExtension::class)
class ReactorRepositoryTest {
    private lateinit var service: ReactorRepository

    private val reflectionHelper = ReflectionHelper(
        EventSourcingConfig(
            eventsPackage = "name.stepin",
            processorsPackage = "name.stepin",
        ),
    )

    @MockK
    lateinit var applicationContext: ApplicationContext

    @BeforeEach
    fun setUp() {
        service = ReactorRepository(reflectionHelper, applicationContext)
    }

    @AfterEach
    fun tearDown() {
        confirmVerified(applicationContext)
    }

    @Test
    fun `process no processors case`() = runBlocking {
        val event = userRegistered(1)
        val meta = EventMetadata()

        service.process(event, meta)
    }

    @Test
    fun `process main case`() = runBlocking {
        val event = userRegistered(1)
        val meta = EventMetadata()
        val processorMock = mockk<UserRegisteredEmailReactor>()
        val processorMock2 = mockk<TestReactor>()
        every { applicationContext.getBean(UserRegisteredEmailReactor::class.java) } returns processorMock
        every { applicationContext.getBean(TestReactor::class.java) } returns processorMock2
        coEvery { processorMock.handle(event) } returns Unit
        coEvery { processorMock2.handle(event, meta) } returns Unit

        service.init()
        service.process(event, meta)

        verify(exactly = 1) { applicationContext.getBean(UserRegisteredEmailReactor::class.java) }
        verify(exactly = 1) { applicationContext.getBean(TestReactor::class.java) }
        coVerify(exactly = 1) { processorMock.handle(event) }
        coVerify(exactly = 1) { processorMock2.handle(event, meta) }
    }
}

@Suppress("unused")
class TestReactor {
    companion object : Logging

    @Suppress("RedundantSuspendModifier", "UNUSED_PARAMETER")
    @Reactor
    suspend fun handle(e: UserRegistered, meta: EventMetadata) {
        // nothing to do
    }
}
