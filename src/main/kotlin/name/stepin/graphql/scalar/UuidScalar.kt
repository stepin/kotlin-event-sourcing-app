package name.stepin.graphql.scalar

import graphql.scalars.ExtendedScalars
import graphql.schema.GraphQLScalarType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.execution.RuntimeWiringConfigurer

@Configuration
class UuidScalar {
    @Bean
    fun runtimeWiringConfigurer(dateTime: GraphQLScalarType): RuntimeWiringConfigurer {
        return RuntimeWiringConfigurer { wiringBuilder ->
            wiringBuilder
                .scalar(ExtendedScalars.UUID)
                .scalar(dateTime)
        }
    }
}
