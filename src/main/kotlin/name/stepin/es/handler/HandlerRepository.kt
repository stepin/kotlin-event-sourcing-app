package name.stepin.es.handler

import name.stepin.es.store.DomainEvent
import name.stepin.es.store.EventMetadata
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.context.ApplicationContext
import kotlin.reflect.KClass

abstract class HandlerRepository(
    private val processorAnnotation: KClass<out Annotation>,
    private val reflectionHelper: ReflectionHelper,
    private val applicationContext: ApplicationContext,
) {
    companion object : Logging

    data class HandlerFunc(
        val bean: Any,
        val methodName: String,
        val withMeta: Boolean,
    )

    protected val map: MutableMap<String, MutableList<HandlerFunc>> = HashMap()
    private val processorType: String = processorAnnotation.simpleName!!

    protected fun initProcessor() {
        val classToEvent = reflectionHelper.classToEvent()
        val methods = reflectionHelper.getMethodsWithAnnotation(processorAnnotation)

        for (method in methods) {
            val (event, withMeta) = reflectionHelper.checkHandlerMethod(method, classToEvent)

            val eventClass = event.canonicalName
            val handlerInstance = applicationContext.getBean(method.declaringClass)
            val handler = HandlerFunc(handlerInstance, method.name, withMeta)

            if (map.containsKey(eventClass)) {
                map[eventClass]!!.add(handler)
            } else {
                map[eventClass] = mutableListOf(handler)
            }

            logger.info {
                val className = handlerInstance.javaClass.toString()
                "New $processorType: $className:${method.name} for $eventClass"
            }
        }
    }

    abstract suspend fun process(event: DomainEvent, meta: EventMetadata)
}
