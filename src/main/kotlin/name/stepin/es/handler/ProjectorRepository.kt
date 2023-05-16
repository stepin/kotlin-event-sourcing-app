package name.stepin.es.handler

import jakarta.annotation.PostConstruct
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service

@Service
class ProjectorRepository(
    reflectionHelper: ReflectionHelper,
    applicationContext: ApplicationContext,
) : HandlerRepository(
    processorAnnotation = Projector::class,
    reflectionHelper = reflectionHelper,
    applicationContext = applicationContext,
) {
    @PostConstruct
    fun init() {
        initProcessor()
    }
}
