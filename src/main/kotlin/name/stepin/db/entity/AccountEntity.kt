package name.stepin.db.entity

import jakarta.validation.constraints.Size
import name.stepin.es.store.AccountGuid
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.io.Serializable
import java.time.Instant
import java.util.*

@Table("accounts")
class AccountEntity : Serializable {
    @Id
    var id: AccountId? = null
    lateinit var guid: AccountGuid

    @Size(max = 128)
    var name: String = ""

    @Column("user_id")
    var userId: UserId = 0

    @Column("created_at")
    lateinit var createdAt: Instant

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AccountEntity) return false

        if (id != other.id) return false
        if (guid != other.guid) return false
        if (name != other.name) return false
        if (userId != other.userId) return false
        return createdAt == other.createdAt
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + guid.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + userId.hashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }

    override fun toString(): String {
        return "AccountEntity(id=$id, guid=$guid, name='$name', userId=$userId, createdAt=$createdAt)"
    }
}
