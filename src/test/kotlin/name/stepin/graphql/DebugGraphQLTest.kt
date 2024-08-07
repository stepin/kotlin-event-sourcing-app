package name.stepin.graphql

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.asFlow
import name.stepin.fixture.AccountEntityFactory.accountEntity
import name.stepin.fixture.UserEntityFactory.userEntity
import name.stepin.graphql.model.AccountResult
import name.stepin.graphql.model.UserResult
import name.stepin.service.DebugService
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.graphql.test.tester.GraphQlTester
import java.time.LocalDateTime
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ExtendWith(MockKExtension::class)
@AutoConfigureGraphQlTester
class DebugGraphQLTest {
    @MockkBean
    private lateinit var debugService: DebugService

    @Autowired
    lateinit var graphQlTester: GraphQlTester

    @AfterEach
    fun tearDown() {
        confirmVerified(debugService)
    }

    @Test
    fun `allUsers main case`() {
        @Language("GraphQL")
        val document =
            """
            query {
                allUsers{name email}
            }
            """.trimIndent()
        coEvery { debugService.getUsers() } returns
            listOf(
                userEntity(1),
                userEntity(2),
            )

        val response =
            graphQlTester.document(document)
                .execute()
                .path("$.data.allUsers")
                .entityList(UserResult::class.java)

        val list = response.get()
        assertEquals(2, list.size)
        val firstElement = list.first()
        assertEquals("displayName1", firstElement.name)
        assertEquals("me1@example.com", firstElement.email)
        coVerify(exactly = 1) { debugService.getUsers() }
    }

    @Test
    fun `usersSince main case`() {
        @Language("GraphQL")
        val document =
            """
            query usersSince1(${'$'}date: DateTime!){
                usersSince(date: ${'$'}date){name email}
            }
            """.trimIndent()
        val dateString = "2023-01-01T01:01:01"
        val date = LocalDateTime.parse(dateString)
        coEvery { debugService.getUsersSince(date) } returns
            listOf(
                userEntity(1),
                userEntity(2),
            )

        val response =
            graphQlTester.document(document)
                .variable("date", dateString)
                .execute()
                .path("$.data.usersSince")
                .entityList(UserResult::class.java)

        val list = response.get()
        assertEquals(2, list.size)
        val firstElement = list.first()
        assertEquals("displayName1", firstElement.name)
        assertEquals("me1@example.com", firstElement.email)
        coVerify(exactly = 1) { debugService.getUsersSince(date) }
    }

    @Test
    fun `userAudit main case`() {
        @Language("GraphQL")
        val document =
            """
            query userAudit(${'$'}userGuid: UUID!){
                userAudit(userGuid: ${'$'}userGuid)
            }
            """.trimIndent()
        val userGuid = UUID.randomUUID()
        val expected =
            listOf(
                "audit1",
                "audit2",
            )
        coEvery { debugService.getUserAudit(userGuid) } returns expected.asFlow()

        val response =
            graphQlTester.document(document)
                .variable("userGuid", userGuid)
                .execute()
                .path("$.data.userAudit")
                .entityList(String::class.java)

        assertEquals(expected, response.get())
        coVerify(exactly = 1) { debugService.getUserAudit(userGuid) }
    }

    @Test
    fun `allAccounts main case`() {
        @Language("GraphQL")
        val document =
            """
            query {
                allAccounts{ name }
            }
            """.trimIndent()
        coEvery { debugService.getAccounts() } returns
            listOf(
                accountEntity(1),
                accountEntity(2),
            )

        val response =
            graphQlTester.document(document)
                .execute()
                .path("$.data.allAccounts")
                .entityList(AccountResult::class.java)
                .get()

        assertEquals(listOf("name1", "name2"), response.map { it.name })
        coVerify(exactly = 1) { debugService.getAccounts() }
    }
}
