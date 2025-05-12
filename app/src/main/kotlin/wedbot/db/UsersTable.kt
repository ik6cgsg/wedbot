package wedbot

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.kotlin.datetime.*

enum class Status { ACCEPT, REJECT, NOT_SURE }
enum class Sex { MALE, FEMALE, NE_BYLO }
enum class Role { ADMIN, DEFAULT }

object Users: Table() {
    val id = integer("id").autoIncrement()
    val chatId = long("chat_id").uniqueIndex().nullable()
    val username = varchar("username", 255).uniqueIndex().nullable()
    val phone = varchar("phone", 13).uniqueIndex().nullable()
    val realName = varchar("real_name", 255).nullable()
    val nikName = varchar("nik_name", 255).nullable()
    val sex = enumerationByName("sex", 8, Sex::class).default(Sex.NE_BYLO)
    val status = enumerationByName("status", 8, Status::class).default(Status.NOT_SURE)
    val role = enumerationByName("role", 8, Role::class).default(Role.DEFAULT)

    override val primaryKey = PrimaryKey(id)
}

data class UserInfo(
    val id: Int,
    var chatId: Long? = null,
    var username: String? = null,
    var phone: String? = null,
    var realName: String? = null,
    var nikName: String? = null,
    var sex: Sex = Sex.NE_BYLO,
    var status: Status = Status.NOT_SURE,
    var role: Role = Role.DEFAULT
)

data class UserStatus(
    val username: String?,
    val phone: String?,
    val realName: String?,
    val status: Status
)

fun ResultRow.toUserInfo() = UserInfo(
    this[Users.id],
    this[Users.chatId],
    this[Users.username],
    this[Users.phone],
    this[Users.realName],
    this[Users.nikName],
    this[Users.sex],
    this[Users.status],
    this[Users.role]
)
