package name.stepin.es.handler

import jakarta.annotation.PostConstruct
import name.stepin.es.store.DomainEvent
import name.stepin.es.store.EventMetadata
import org.apache.logging.log4j.kotlin.Logging
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
    companion object : Logging

    @PostConstruct
    fun init() {
        initProcessor()
    }

    override suspend fun process(event: DomainEvent, meta: EventMetadata) {
        val eventClass = event.javaClass.canonicalName
        val processors = map[eventClass]
        if (processors.isNullOrEmpty()) {
            logger.info { "No projectors for $eventClass" }
            return
        }
        for ((processor, methodName, withMeta) in processors) {
            logger.debug { "Projector ${processor::class.java} $methodName $withMeta" }
            if (withMeta) {
                processor.invokeSuspendFunction(methodName, event, meta)
            } else {
                processor.invokeSuspendFunction(methodName, event)
            }
        }
    }
}
