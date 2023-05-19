@file:JvmName(name = "JooqKotlin")

package name.stepin.utils

import kotlinx.coroutines.reactive.awaitFirst
import org.jooq.DSLContext
import org.jooq.TableRecord

suspend fun <R : TableRecord<R>> DSLContext.coInsert(record: R): Int {
    return insertInto(record.table).set(record).awaitFirst()
}
