package name.stepin.es.processor

import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import name.stepin.config.EventSourcingConfig
import name.stepin.config.StartupType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.boot.actuate.health.Status

@ExtendWith(MockKExtension::class)
class EventSourcingStarterTest {
    private lateinit var service: EventSourcingStarter

    @MockK
    lateinit var config: EventSourcingConfig

    @MockK
    lateinit var seed: Seed

    @MockK
    lateinit var inlineProcessor: InlineProcessor

    @BeforeEach
    fun setUp() {
        service = EventSourcingStarter(config, seed, inlineProcessor)
        service.reset()
    }

    @AfterEach
    fun tearDown() {
        confirmVerified(config, seed, inlineProcessor)
    }

    @Test
    fun `health down by default`() {
        val actual = service.health()

        assertEquals(Status.DOWN, actual.status)
    }

    @Test
    fun `run SEED case`() {
        val args = DefaultApplicationArguments()
        every { config.startupType } returns StartupType.SEED
        coEvery { seed.run() } returns true

        service.run(args)
        val actual = service.health()

        assertEquals(Status.UP, actual.status)
        verify(exactly = 1) { config.startupType }
        coVerify(exactly = 1) { seed.run() }
    }

    @Test
    fun `run DB_PROCESSING case`() {
        val args = DefaultApplicationArguments()
        every { config.startupType } returns StartupType.DB_PROCESSING
        every { config.dbProcessingFrom } returns 123
        every { config.dbProcessingTo } returns 456
        coEvery { inlineProcessor.replayEvents(skipReactor = true, from = 123, to = 456) } returns Unit

        service.run(args)
        val actual = service.health()

        assertEquals(Status.UP, actual.status)
        verify(exactly = 1) { config.startupType }
        verify(exactly = 1) { config.dbProcessingFrom }
        verify(exactly = 1) { config.dbProcessingTo }
        coVerify(exactly = 1) { inlineProcessor.replayEvents(skipReactor = true, from = 123, to = 456) }
    }

    @Test
    fun `run NO_INITIAL_PROCESSING case`() {
        val args = DefaultApplicationArguments()
        every { config.startupType } returns StartupType.NO_INITIAL_PROCESSING

        service.run(args)
        val actual = service.health()

        assertEquals(Status.UP, actual.status)
        verify(exactly = 1) { config.startupType }
    }
}
