package name.stepin.domain.user.event

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

class UserEventTest {
    @Test
    fun `check main fields`() {
        val event: UserEvent = UserRemoved(aggregatorGuid = UUID.randomUUID(), accountGuid = UUID.randomUUID())

        assertEquals("user", event.aggregatorType)
        assertEquals(0, event.eventTypeVersion)
        assertEquals("UserRemoved", event.eventType)
    }
}
