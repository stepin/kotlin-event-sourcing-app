package name.stepin.es.handler

import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.isAccessible

suspend fun Any.invokeSuspendFunction(
    methodName: String,
    vararg args: Any?,
): Any? =
    this::class.memberFunctions.find { it.name == methodName }?.also {
        it.isAccessible = true
        return it.callSuspend(this, *args)
    }
