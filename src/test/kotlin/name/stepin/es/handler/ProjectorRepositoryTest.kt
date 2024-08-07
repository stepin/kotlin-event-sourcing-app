package name.stepin.es.handler

import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import name.stepin.config.EventSourcingConfig
import name.stepin.domain.account.projector.AccountCreatedProjector
import name.stepin.domain.user.event.UserRegistered
import name.stepin.domain.user.projector.UserProjector
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
class ProjectorRepositoryTest {
    private lateinit var service: ProjectorRepository

    private val reflectionHelper =
        ReflectionHelper(
            EventSourcingConfig(
                eventsPackage = "name.stepin",
                processorsPackage = "name.stepin",
            ),
        )

    @MockK
    lateinit var applicationContext: ApplicationContext

    @BeforeEach
    fun setUp() {
        service = ProjectorRepository(reflectionHelper, applicationContext)
    }

    @AfterEach
    fun tearDown() {
        confirmVerified(applicationContext)
    }

    @Test
    fun `process no processors case`() =
        runBlocking {
            val event = userRegistered(1)
            val meta = EventMetadata()

            service.process(event, meta)
        }

    @Test
    fun `process main case`() =
        runBlocking {
            val event = userRegistered(1)
            val meta = EventMetadata()
            val processorMock = mockk<UserProjector>()
            val processorMock2 = mockk<TestProjector>()
            val processorMock3 = mockk<AccountCreatedProjector>()
            every { applicationContext.getBean(UserProjector::class.java) } returns processorMock
            every { applicationContext.getBean(TestProjector::class.java) } returns processorMock2
            every { applicationContext.getBean(AccountCreatedProjector::class.java) } returns processorMock3
            coEvery { processorMock.handleUserRegistered(event, meta) } returns Unit
            coEvery { processorMock2.handle(event) } returns Unit

            service.init()
            service.process(event, meta)

            verify(exactly = 3) { applicationContext.getBean(UserProjector::class.java) }
            verify(exactly = 1) { applicationContext.getBean(TestProjector::class.java) }
            verify(exactly = 1) { applicationContext.getBean(AccountCreatedProjector::class.java) }
            coVerify(exactly = 1) { processorMock.handleUserRegistered(event, meta) }
            coVerify(exactly = 1) { processorMock2.handle(event) }
        }
}

@Suppress("unused")
class TestProjector {
    companion object : Logging

    @Suppress("RedundantSuspendModifier", "UNUSED_PARAMETER")
    @Projector
    suspend fun handle(e: UserRegistered) {
        // nothing to do
    }
}
