package name.stepin.es.handler

import name.stepin.config.EventSourcingConfig
import name.stepin.domain.account.event.AccountCreated
import name.stepin.domain.user.event.UserRegistered
import name.stepin.es.store.EventMetadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.reflect.Method

class ReflectionHelperTest {

    private lateinit var helper: ReflectionHelper

    @BeforeEach
    fun setUp() {
        helper = ReflectionHelper(EventSourcingConfig())
    }

    @Test
    fun `checkHandlerMethod no args case`() {
        val method = getMethodByName("noArgs")

        val exception = assertThrows<IllegalStateException> { helper.checkHandlerMethod(method, emptyMap()) }

        assertEquals(
            "Incorrect number of params for handler function " +
                "public final void name.stepin.es.handler.TestClass.noArgs()",
            exception.message,
        )
    }

    @Test
    fun `checkHandlerMethod 2 params not suspend function`() {
        val method = getMethodByName("notSuspend2args")

        val exception = assertThrows<IllegalStateException> { helper.checkHandlerMethod(method, emptyMap()) }

        assertEquals(
            "If 2d param exists it should be EventMetadata or Continuation " +
                "public final void name.stepin.es.handler.TestClass.notSuspend2args(int,int)",
            exception.message,
        )
    }

    @Test
    fun `checkHandlerMethod 3 params 2nd should be meta`() {
        val method = getMethodByName("notMeta3args")

        val exception = assertThrows<IllegalStateException> { helper.checkHandlerMethod(method, emptyMap()) }

        assertEquals(
            "If second param exists it should be EventMetadata or Continuation " +
                "public final void name.stepin.es.handler.TestClass.notMeta3args(int,int,int)",
            exception.message,
        )
    }

    @Test
    fun `checkHandlerMethod 3 params not suspend function`() {
        val method = getMethodByName("notSuspend3args")

        val exception = assertThrows<IllegalStateException> { helper.checkHandlerMethod(method, emptyMap()) }

        assertEquals(
            "If 3d param exists it should be Continuation " +
                "public final void name.stepin.es.handler.TestClass.notSuspend3args" +
                "(int,name.stepin.es.store.EventMetadata,int)",
            exception.message,
        )
    }

    @Test
    fun `checkHandlerMethod 2 params not event arg`() {
        val method = getMethodByName("notEvent1")

        val exception = assertThrows<IllegalStateException> { helper.checkHandlerMethod(method, emptyMap()) }

        assertEquals(
            "1st param should be event " +
                "public final java.lang.Object name.stepin.es.handler.TestClass.notEvent1" +
                "(int,kotlin.coroutines.Continuation)",
            exception.message,
        )
    }

    @Test
    fun `checkHandlerMethod 3 params not event arg`() {
        val method = getMethodByName("notEvent2")

        val exception = assertThrows<IllegalStateException> { helper.checkHandlerMethod(method, emptyMap()) }

        assertEquals(
            "1st param should be event " +
                "public final java.lang.Object name.stepin.es.handler.TestClass.notEvent2" +
                "(int,name.stepin.es.store.EventMetadata,kotlin.coroutines.Continuation)",
            exception.message,
        )
    }

    @Test
    fun `checkHandlerMethod 2 params correct function`() {
        val method = getMethodByName("correct1")
        val eventsMap: Map<Class<*>, String> = mapOf(
            UserRegistered::class.java to "UserRegistered",
            AccountCreated::class.java to "AccountCreated",
        )

        val (actualClass, actualWithMete) = helper.checkHandlerMethod(method, eventsMap)

        assertEquals(UserRegistered::class.java, actualClass)
        assertEquals(false, actualWithMete)
    }

    @Test
    fun `checkHandlerMethod 3 params correct function`() {
        val method = getMethodByName("correct2")
        val eventsMap: Map<Class<*>, String> = mapOf(
            UserRegistered::class.java to "UserRegistered",
            AccountCreated::class.java to "AccountCreated",
        )

        val (actualClass, actualWithMete) = helper.checkHandlerMethod(method, eventsMap)

        assertEquals(AccountCreated::class.java, actualClass)
        assertEquals(true, actualWithMete)
    }

    private fun getMethodByName(name: String): Method {
        return TestClass::class.java.methods.find { it.name == name }!!
    }
}

@Suppress("unused", "UNUSED_PARAMETER", "RedundantSuspendModifier")
internal class TestClass {

    fun noArgs() {
    }

    fun notSuspend2args(a: Int, b: Int) {
    }

    fun notMeta3args(a: Int, b: Int, c: Int) {
    }

    fun notSuspend3args(a: Int, meta: EventMetadata, c: Int) {
    }

    suspend fun notEvent1(a: Int) {
    }

    suspend fun notEvent2(a: Int, meta: EventMetadata) {
    }

    suspend fun correct1(event: UserRegistered) {
    }

    suspend fun correct2(event: AccountCreated, meta: EventMetadata) {
    }
}
