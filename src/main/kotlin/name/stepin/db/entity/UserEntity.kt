package name.stepin.db.entity

import jakarta.validation.constraints.Size
import name.stepin.es.store.AccountGuid
import name.stepin.es.store.UserGuid
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.io.Serializable
import java.time.Instant
import java.util.*

@Table("users")
class UserEntity : Serializable {
    @Id
    var id: UserId? = null
    lateinit var guid: UserGuid

    @Column("account_id")
    var accountId: AccountId = 0

    @Column("account_guid")
    lateinit var accountGuid: AccountGuid

    @Column("display_name")
    @Size(max = 128)
    var displayName: String? = null

    @Column("first_name")
    @Size(max = 128)
    var firstName: String? = null

    @Column("second_name")
    @Size(max = 128)
    var secondName: String? = null

    @Size(max = 128)
    lateinit var email: String

    @Column("created_at")
    lateinit var createdAt: Instant

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserEntity) return false

        if (id != other.id) return false
        if (guid != other.guid) return false
        if (accountId != other.accountId) return false
        if (accountGuid != other.accountGuid) return false
        if (displayName != other.displayName) return false
        if (firstName != other.firstName) return false
        if (secondName != other.secondName) return false
        if (email != other.email) return false
        return createdAt == other.createdAt
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + guid.hashCode()
        result = 31 * result + accountId.hashCode()
        result = 31 * result + accountGuid.hashCode()
        result = 31 * result + (displayName?.hashCode() ?: 0)
        result = 31 * result + (firstName?.hashCode() ?: 0)
        result = 31 * result + (secondName?.hashCode() ?: 0)
        result = 31 * result + email.hashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }

    override fun toString(): String {
        return "UserEntity(id=$id, guid=$guid, accountId=$accountId, accountGuid=$accountGuid, " +
            "displayName=$displayName, firstName=$firstName, secondName=$secondName, email='$email', " +
            "createdAt=$createdAt)"
    }
}
