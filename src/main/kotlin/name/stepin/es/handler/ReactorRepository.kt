package name.stepin.es.handler

import jakarta.annotation.PostConstruct
import name.stepin.es.store.DomainEvent
import name.stepin.es.store.EventMetadata
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import java.lang.reflect.InvocationTargetException

@Service
class ReactorRepository(
    reflectionHelper: ReflectionHelper,
    applicationContext: ApplicationContext,
) : HandlerRepository(
    processorAnnotation = Reactor::class,
    reflectionHelper = reflectionHelper,
    applicationContext = applicationContext,
) {
    @PostConstruct
    fun init() {
        initProcessor()
    }

    override suspend fun process(event: DomainEvent, meta: EventMetadata) {
        val eventClass = event.javaClass.canonicalName
        val processors = map[eventClass]
        if (processors.isNullOrEmpty()) {
            logger.info { "No reactors for $eventClass" }
            return
        }
        for ((processor, methodName, withMeta) in processors) {
            try {
                if (withMeta) {
                    processor.invokeSuspendFunction(methodName, event, meta)
                } else {
                    processor.invokeSuspendFunction(methodName, event)
                }
            } catch (e: RuntimeException) {
                logger.error("reactor runtime error $event $meta", e)
            } catch (e: InvocationTargetException) {
                logger.error("reactor target error $event $meta", e)
            }
        }
    }
}
