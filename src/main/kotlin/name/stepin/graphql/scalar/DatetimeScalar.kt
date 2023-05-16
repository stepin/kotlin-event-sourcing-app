package name.stepin.graphql.scalar

import graphql.language.StringValue
import graphql.schema.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

@Configuration
class DatetimeScalar {
    /**
     * NOTE: ExtendedScalars don't have scalar for LocalDateTime.
     * Only for OffsetDateTime, LocalTime, and LocalDate.
     */
    @Bean
    fun dateTime(): GraphQLScalarType {
        return GraphQLScalarType.newScalar()
            .name("DateTime")
            .description("Java 8 LocalDatetime as scalar")
            .coercing(
                object : Coercing<LocalDateTime?, String?> {
                    override fun serialize(dataFetcherResult: Any): String {
                        return (dataFetcherResult as? LocalDateTime)?.toString()
                            ?: throw CoercingSerializeException("Expected a LocalDatetime object.")
                    }

                    override fun parseValue(input: Any): LocalDateTime {
                        return try {
                            if (input is String) {
                                LocalDateTime.parse(input)
                            } else {
                                throw CoercingParseValueException("Expected a String.")
                            }
                        } catch (e: DateTimeParseException) {
                            throw CoercingParseValueException("Not a valid datetime: '$input'.", e)
                        }
                    }

                    override fun parseLiteral(input: Any): LocalDateTime {
                        return if (input is StringValue) {
                            try {
                                LocalDateTime.parse(input.value)
                            } catch (e: DateTimeParseException) {
                                throw CoercingParseLiteralException("Not a valid datetime: '${input.value}'.", e)
                            }
                        } else {
                            throw CoercingParseLiteralException("Expected a StringValue.")
                        }
                    }
                },
            ).build()
    }
}
