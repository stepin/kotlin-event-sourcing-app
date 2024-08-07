package name.stepin.web

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.junit5.MockKExtension
import name.stepin.fixture.AccountEntityFactory.accountEntity
import name.stepin.fixture.UserEntityFactory.userEntity
import name.stepin.service.DebugService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

@ExtendWith(MockKExtension::class)
@WebFluxTest(controllers = [DebugController::class])
class DebugControllerTest {
    @MockkBean
    private lateinit var debugService: DebugService

    @Autowired
    lateinit var client: WebTestClient

    @AfterEach
    fun tearDown() {
        confirmVerified(debugService)
    }

    @Test
    fun `allUsers main case`() {
        coEvery { debugService.getUsers() } returns
            listOf(
                userEntity(1),
                userEntity(2),
            )

        val response =
            client.get()
                .uri("/api/debug/users")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()

        response
            .expectStatus().isOk
            .expectBody()
            .json(
                """
                [
                  {"name":"displayName1","email":"me1@example.com"},
                  {"name":"displayName2","email":"me2@example.com"}
                ]
                """.trimIndent(),
            )
        coVerify(exactly = 1) { debugService.getUsers() }
    }

    @Test
    fun `allAccounts main case`() {
        coEvery { debugService.getAccounts() } returns
            listOf(
                accountEntity(1),
                accountEntity(2),
            )

        val response =
            client.get()
                .uri("/api/debug/accounts")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()

        response
            .expectStatus().isOk
            .expectBody()
            .json(
                """
                [
                  {"name":"name1"},
                  {"name":"name2"}
                ]
                """.trimIndent(),
            )
        coVerify(exactly = 1) { debugService.getAccounts() }
    }
}
