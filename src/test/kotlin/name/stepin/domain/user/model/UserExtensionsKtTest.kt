package name.stepin.domain.user.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UserExtensionsKtTest {

    @Test
    fun `calcDisplayName firstName and secondName are null`() {
        val actual = calcDisplayName("email1@example.com", null, null)

        assertEquals("email1@example.com", actual)
    }

    @Test
    fun `calcDisplayName firstName is null`() {
        val actual = calcDisplayName("email1@example.com", null, "lastName1")

        assertEquals("lastName1", actual)
    }

    @Test
    fun `calcDisplayName secondName is null`() {
        val actual = calcDisplayName("email1@example.com", "firstName1", null)

        assertEquals("firstName1", actual)
    }

    @Test
    fun `calcDisplayName all are not null`() {
        val actual = calcDisplayName("email1@example.com", "firstName1", "lastName1")

        assertEquals("firstName1 lastName1", actual)
    }
}
