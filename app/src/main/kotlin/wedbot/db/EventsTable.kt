package wedbot

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.kotlin.datetime.*

object InviteEvents: Table() {
    val id = integer("id").autoIncrement()
    val initiatorСhatId = long("init_chat_id")
    val invitedСhatId = long("inv_chat_id")
    val username = varchar("username", 255).nullable()
    val realName = varchar("real_name", 255).nullable()
    val userConfirmed = bool("user_confirmed").nullable()
    val adminConfirmed = bool("admin_confirmed").nullable()
    val isCompleted = bool("is_completed").default(false)

    override val primaryKey = PrimaryKey(id)
}

data class InviteEventInfo(
    val id: Int,
    val initiatorСhatId: Long,
    val invitedСhatId: Long,
    var invitedUsername: String? = null,
    var invitedRealName: String? = null,
    var userConfirmed: Boolean? = null,
    var adminConfirmed: Boolean? = null,
    var isCompleted: Boolean = false,
)

fun ResultRow.toInviteEventInfo() = InviteEventInfo(
    this[InviteEvents.id],
    this[InviteEvents.initiatorСhatId],
    this[InviteEvents.invitedСhatId],
    this[InviteEvents.username],
    this[InviteEvents.realName],
    this[InviteEvents.userConfirmed],
    this[InviteEvents.adminConfirmed],
    this[InviteEvents.isCompleted]
)
