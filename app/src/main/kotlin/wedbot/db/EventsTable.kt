package wedbot

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.kotlin.datetime.*

object InviteEvents: Table() {
    val id = integer("id").autoIncrement()
    val initiatorСhatId = long("init_chat_id")
    val invitedСhatId = long("inv_chat_id")
    val invitedUsername = varchar("inv_username", 255).nullable()
    val invitedRealName = varchar("inv_real_name", 255).nullable()
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
    this[InviteEvents.invitedUsername],
    this[InviteEvents.invitedRealName],
    this[InviteEvents.userConfirmed],
    this[InviteEvents.adminConfirmed],
    this[InviteEvents.isCompleted]
)
