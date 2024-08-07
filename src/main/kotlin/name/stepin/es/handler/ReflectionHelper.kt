package name.stepin.es.handler

import name.stepin.config.EventSourcingConfig
import name.stepin.es.store.DomainEvent
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.springframework.stereotype.Service
import java.lang.reflect.Method
import kotlin.reflect.KClass

@Service
class ReflectionHelper(
    eventSourcingConfig: EventSourcingConfig,
) {
    private val eventsPackage: String = eventSourcingConfig.eventsPackage
    private val processorsPackage: String = eventSourcingConfig.processorsPackage

    fun eventsToClass(): HashMap<String, Class<*>> {
        val reflections = Reflections(eventsPackage)
        val eventClasses: Set<Class<out DomainEvent?>> = reflections.getSubTypesOf(DomainEvent::class.java)
        val event2class = HashMap<String, Class<*>>()
        for (eventClass in eventClasses) {
            event2class[eventClass.simpleName] = eventClass
        }
        return event2class
    }

    fun classToEvent(): HashMap<Class<*>, String> {
        val reflections = Reflections(eventsPackage)
        val eventClasses: Set<Class<out DomainEvent?>> = reflections.getSubTypesOf(DomainEvent::class.java)
        val classToEvent = HashMap<Class<*>, String>()
        for (eventClass in eventClasses) {
            classToEvent[eventClass] = eventClass.simpleName
        }
        return classToEvent
    }

    fun getMethodsWithAnnotation(processorAnnotation: KClass<out Annotation>): Set<Method> {
        val reflections = Reflections(processorsPackage, Scanners.MethodsAnnotated)
        return reflections.getMethodsAnnotatedWith(processorAnnotation.java)
    }

    fun checkHandlerMethod(
        method: Method,
        classToEvent: Map<Class<*>, String>,
    ): Pair<Class<*>, Boolean> {
        val paramsCount = method.parameterTypes.size
        if (paramsCount != 2 && paramsCount != 3) {
            throw IllegalStateException("Incorrect number of params for handler function $method")
        }
        val withMeta: Boolean
        if (paramsCount == 2) {
            val kotlinContinuation = method.parameterTypes[1]
            if (kotlinContinuation.name != "kotlin.coroutines.Continuation") {
                throw IllegalStateException("If 2d param exists it should be EventMetadata or Continuation $method")
            }
            withMeta = false
        } else {
            val meta = method.parameterTypes[1]
            if (meta.name != "name.stepin.es.store.EventMetadata") {
                throw IllegalStateException("If second param exists it should be EventMetadata or Continuation $method")
            }
            val kotlinContinuation = method.parameterTypes[2]
            if (kotlinContinuation.name != "kotlin.coroutines.Continuation") {
                throw IllegalStateException("If 3d param exists it should be Continuation $method")
            }
            withMeta = true
        }
        val event = method.parameterTypes[0]
        if (!classToEvent.containsKey(event)) {
            throw IllegalStateException("1st param should be event $method")
        }
        return event to withMeta
    }
}
