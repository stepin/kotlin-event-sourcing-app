package name.stepin.graphql.scalar

import graphql.language.StringValue
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
import graphql.schema.GraphQLScalarType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

class DatetimeScalarTest {
    private lateinit var datetimeType: GraphQLScalarType

    @BeforeEach
    fun setUp() {
        datetimeType = DatetimeScalar().dateTime()
    }

    @Test
    fun `serialize wrong type`() {
        val obj = 1L

        val exception = assertThrows<CoercingSerializeException> { datetimeType.coercing.serialize(obj) }

        assertEquals("Expected a LocalDatetime object.", exception.message)
    }

    @Test
    fun `serialize main case`() {
        val obj = LocalDateTime.of(2023, 1, 1, 1, 1, 1)

        val actual = datetimeType.coercing.serialize(obj)

        assertEquals("2023-01-01T01:01:01", actual)
    }

    @Test
    fun `parseValue wrong type`() {
        val obj = 1L

        val exception = assertThrows<CoercingParseValueException> { datetimeType.coercing.parseValue(obj) }

        assertEquals("Expected a String.", exception.message)
    }

    @Test
    fun `parseValue date parsing error case`() {
        val exception =
            assertThrows<CoercingParseValueException> {
                datetimeType.coercing.parseValue("Not a date string")
            }

        assertEquals("Not a valid datetime: 'Not a date string'.", exception.message)
    }

    @Test
    fun `parseValue main case`() {
        val expected = LocalDateTime.of(2023, 1, 1, 1, 1, 1)

        val actual = datetimeType.coercing.parseValue("2023-01-01T01:01:01")

        assertEquals(expected, actual)
    }

    @Test
    fun `parseLiteral wrong type`() {
        val obj = 1L

        val exception = assertThrows<CoercingParseLiteralException> { datetimeType.coercing.parseLiteral(obj) }

        assertEquals("Expected a StringValue.", exception.message)
    }

    @Test
    fun `parseLiteral date parsing error case`() {
        val exception =
            assertThrows<CoercingParseLiteralException> {
                datetimeType.coercing.parseLiteral(StringValue("Not a date string"))
            }

        assertEquals("Not a valid datetime: 'Not a date string'.", exception.message)
    }

    @Test
    fun `parseLiteral main case`() {
        val expected = LocalDateTime.of(2023, 1, 1, 1, 1, 1)

        val actual = datetimeType.coercing.parseLiteral(StringValue("2023-01-01T01:01:01"))

        assertEquals(expected, actual)
    }
}
