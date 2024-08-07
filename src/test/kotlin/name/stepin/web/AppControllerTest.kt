package name.stepin.web

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.junit5.MockKExtension
import name.stepin.domain.user.command.RegisterUser
import name.stepin.domain.user.command.RemoveUser
import name.stepin.domain.user.command.UpdateUserInformation
import name.stepin.exception.ErrorCode
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.*

@ExtendWith(MockKExtension::class)
@WebFluxTest(controllers = [AppController::class])
class AppControllerTest {
    @MockkBean
    private lateinit var registerUser: RegisterUser

    @MockkBean
    private lateinit var updateUserInformation: UpdateUserInformation

    @MockkBean
    private lateinit var removeUser: RemoveUser

    @Autowired
    lateinit var client: WebTestClient

    @AfterEach
    fun tearDown() {
        confirmVerified(registerUser, updateUserInformation, removeUser)
    }

    @Test
    fun `registerUser user already registered case`() {
        val params =
            RegisterUser.Params(
                email = "corey.kent@example.com",
                firstName = "firstName1",
                secondName = "secondName1",
                displayName = "displayName1",
            )
        coEvery { registerUser.execute(params) } returns RegisterUser.Response.Error(ErrorCode.USER_ALREADY_REGISTERED)

        @Language("JSON")
        val body =
            """
            {
            "email": "corey.kent@example.com",
            "firstName": "firstName1",
            "secondName": "secondName1",
            "displayName": "displayName1"
            }
            """.trimIndent()

        val response =
            client.post()
                .uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()

        response
            .expectStatus().is5xxServerError
            .expectBody()
            .json(
                """
                {
                  "status": 500,
                  "error": "Internal Server Error"
                }
                """.trimIndent(),
            )
        coVerify(exactly = 1) { registerUser.execute(params) }
    }

    @Test
    fun `registerUser success case`() {
        val params =
            RegisterUser.Params(
                email = "corey.kent@example.com",
                firstName = "firstName1",
                secondName = "secondName1",
                displayName = "displayName1",
            )
        val userGuid = UUID.randomUUID()
        coEvery { registerUser.execute(params) } returns RegisterUser.Response.Created(userGuid)

        @Language("JSON")
        val body =
            """
            {
            "email": "corey.kent@example.com",
            "firstName": "firstName1",
            "secondName": "secondName1",
            "displayName": "displayName1"
            }
            """.trimIndent()

        val response =
            client.post()
                .uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()

        response
            .expectStatus().isOk
            .expectBody()
            .json(
                """
                "$userGuid"
                """.trimIndent(),
            )
        coVerify(exactly = 1) { registerUser.execute(params) }
    }

    @Test
    fun `updateUserInformation success case`() {
        val userGuid = UUID.randomUUID()
        val params =
            UpdateUserInformation.Params(
                userGuid = userGuid,
                firstName = "firstName2",
                secondName = null,
                displayName = null,
            )
        coEvery { updateUserInformation.execute(params) } returns null

        @Language("JSON")
        val body =
            """
            {
            "firstName": "firstName2"
            }
            """.trimIndent()

        val response =
            client.post()
                .uri("/api/users/$userGuid")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()

        response
            .expectStatus().isOk
        coVerify(exactly = 1) { updateUserInformation.execute(params) }
    }

    @Test
    fun `removeUser success case`() {
        val userGuid = UUID.randomUUID()
        coEvery { removeUser.execute(userGuid) } returns null

        val response =
            client.delete()
                .uri("/api/users/$userGuid")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()

        response
            .expectStatus().isOk
        coVerify(exactly = 1) { removeUser.execute(userGuid) }
    }
}
