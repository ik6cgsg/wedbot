package wedbot.data.repository

import wedbot.data.db.DatabaseSqlite
import wedbot.domain.entity.UserInfo
import wedbot.domain.entity.UserStatus
import wedbot.domain.repository.UserRepository

class ExposedUserRepository(
    private val db: DatabaseSqlite
) : UserRepository {
    override fun getByUsername(username: String): Result<UserInfo> = runCatching {
        db.getUserByUsername(username) ?: throw RuntimeException()
    }

    override fun getByPhone(phone: String): Result<UserInfo> = runCatching {
        db.getUserByPhone(phone) ?: throw RuntimeException()
    }

    override fun getByChatId(chatId: Long): Result<UserInfo> = runCatching {
        db.getUserByChatId(chatId) ?: throw RuntimeException()
    }

    override fun getAdminChatIds(): List<Long> {
        TODO("Not yet implemented")
    }

    override fun getAllUserChatIds(): List<Long> {
        TODO("Not yet implemented")
    }

    override fun create(newUser: UserInfo) {
        try {
            db.create(newUser)
        } catch (_: Throwable) {}
    }

    override fun update(user: UserInfo) {
        try {
            db.update(user)
        } catch (_: Throwable) {}
    }

    override fun getStatuses(
        offset: Long?,
        limit: Int?
    ): List<UserStatus> {
        return try {
            db.getUserStatuses(offset, limit)
        } catch (_: Throwable) {
            listOf()
        }
    }
}
