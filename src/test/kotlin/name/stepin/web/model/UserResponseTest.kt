package name.stepin.web.model

import name.stepin.fixture.UserEntityFactory.userEntity
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class UserResponseTest {
    @Test
    fun `default name`() {
        val entity =
            userEntity(1).apply {
                displayName = null
            }

        val actual = UserResponse.from(entity)

        assertEquals(entity.email, actual.email)
        assertEquals("<none>", actual.name)
    }
}
