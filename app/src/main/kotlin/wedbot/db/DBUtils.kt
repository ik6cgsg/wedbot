package wedbot

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.kotlin.datetime.*
import java.io.File

class DBUtils {
    private val dbPath = "jdbc:sqlite:res/data.db"
    private val driver = "org.sqlite.JDBC"

    init {
        Database.connect(dbPath, driver)
        transaction {
            SchemaUtils.create(Users)
            SchemaUtils.create(InviteEvents)
        }
        // TODO: from csv ??
        //initGen()
        executeSqlFile("res/init.sql")
    }

    private fun executeSqlFile(filePath: String) {
        transaction {
            val sqlScript = File(filePath).readText()
            sqlScript.split(";")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .forEach { query ->
                    if (query.isNotEmpty()) {
                        exec(query)
                    }
                }
        }
    }

    // MARK: Users

    fun getUserByUsername(username: String): UserInfo? {
        return transaction {
            Users.selectAll()
                .where { Users.username eq username }
                .singleOrNull()
                ?.toUserInfo()
        }
    }

    fun getUserByPhone(phone: String): UserInfo? {
        return transaction {
            Users.selectAll()
                .where { Users.phone eq phone }
                .singleOrNull()
                ?.toUserInfo()
        }
    }

    fun getUserByChatId(chatId: Long): UserInfo? {
        return transaction {
            Users.selectAll()
                .where { Users.chatId eq chatId }
                .singleOrNull()
                ?.toUserInfo()
        }
    }

    fun getAdminChats(): List<Long> {
        return transaction {
            Users.select(Users.chatId)
                .where { Users.role eq Role.ADMIN and Users.chatId.isNotNull() }
                .map { it[Users.chatId]!! }
        }
    }

    fun createUser(chatId: Long, username: String?, realName: String?) {
        transaction {
            Users.insert {
                it[Users.chatId] = chatId
                it[Users.username] = username
                it[Users.realName] = realName
            }
        }
    }

    fun updateUser(updatedInfo: UserInfo) {
        transaction {
            Users.update({ Users.id eq updatedInfo.id }) {
                it[Users.chatId] = updatedInfo.chatId
                it[Users.username] = updatedInfo.username
                it[Users.phone] = updatedInfo.phone
                it[Users.realName] = updatedInfo.realName
                it[Users.nikName] = updatedInfo.nikName
                it[Users.sex] = updatedInfo.sex
                it[Users.status] = updatedInfo.status
                it[Users.role] = updatedInfo.role
            }
        }
    }

    fun getUserStatuses(offset: Long, limit: Int): List<UserStatus> {
        return transaction {
            Users.select(Users.username, Users.phone, Users.realName, Users.status)
                .offset(offset)
                .limit(limit)
                .map { UserStatus(
                    it[Users.username], it[Users.phone], it[Users.realName], it[Users.status]
                ) }
        }
    }

    // MARK: Invite Events

    fun createInviteEvent(initChatId: Long, invСhatId: Long) {
        transaction {
            InviteEvents.insert {
                it[initiatorСhatId] = initChatId
                it[invitedСhatId] = invСhatId
            }
        }
    }

    fun getActiveInviteByChatId(invСhatId: Long): InviteEventInfo? {
        return transaction {
            InviteEvents.selectAll()
                .where { InviteEvents.invitedСhatId eq invСhatId and not(InviteEvents.isCompleted) }
                .singleOrNull()
                ?.toInviteEventInfo()
        }
    }

    fun getActiveInviteById(inviteId: Int): InviteEventInfo? {
        return transaction {
            InviteEvents.selectAll()
                .where { InviteEvents.id eq inviteId and not(InviteEvents.isCompleted) }
                .singleOrNull()
                ?.toInviteEventInfo()
        }
    }

    fun updateInviteEvent(updatedInfo: InviteEventInfo) {
        transaction {
            InviteEvents.update({ InviteEvents.id eq updatedInfo.id }) {
                it[InviteEvents.initiatorСhatId] = updatedInfo.initiatorСhatId
                it[InviteEvents.invitedСhatId] = updatedInfo.invitedСhatId
                it[InviteEvents.invitedUsername] = updatedInfo.invitedUsername
                it[InviteEvents.invitedRealName] = updatedInfo.invitedRealName
                it[InviteEvents.userConfirmed] = updatedInfo.userConfirmed
                it[InviteEvents.adminConfirmed] = updatedInfo.adminConfirmed
                it[InviteEvents.isCompleted] = updatedInfo.isCompleted
            }
        }
    }
}

fun DBUtils.initGen() {
    transaction {
        Users.insert {
            it[username] = "cgsgilich"
            it[realName] = "илья"
            it[role] = Role.ADMIN
        }
    }
}
