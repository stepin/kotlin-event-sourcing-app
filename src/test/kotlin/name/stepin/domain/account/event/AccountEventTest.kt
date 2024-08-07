package name.stepin.domain.account.event

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

class AccountEventTest {
    @Test
    fun `check main fields`() {
        val event: AccountEvent = AccountCreated(name = "Francine McBride", userGuid = UUID.randomUUID())

        assertEquals("account", event.aggregatorType)
        assertEquals(0, event.eventTypeVersion)
        assertEquals("AccountCreated", event.eventType)
    }
}
