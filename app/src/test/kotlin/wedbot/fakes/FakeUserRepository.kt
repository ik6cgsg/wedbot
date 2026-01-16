package wedbot.fakes

import wedbot.domain.entity.Role
import wedbot.domain.entity.Status
import wedbot.domain.entity.UserInfo
import wedbot.domain.entity.UserStatus
import wedbot.domain.entity.toUserStatus
import wedbot.domain.repository.UserRepository

class FakeUserRepository(
    initialUsers: List<UserInfo> = emptyList()
) : UserRepository {

    private val users = initialUsers.toMutableList()

    override fun getByUsername(username: String): Result<UserInfo> = runCatching {
        users.find { it.username == username } ?: throw Exception("User not found")
    }

    override fun getByPhone(phone: String): Result<UserInfo> = runCatching {
        users.find { it.phone == phone } ?: throw Exception("User not found")
    }

    override fun getByChatId(chatId: Long): Result<UserInfo> = runCatching {
        users.find { it.chatId == chatId } ?: throw Exception("User not found")
    }

    override fun getAdminChatIds(): Result<List<Long>> = runCatching {
        val res = users
            .filter { it.role == Role.ADMIN }
            .mapNotNull { it.chatId }
        if (res.isEmpty()) throw Exception("No admins found")
        res
    }

    override fun getAllUserChatIds(): Result<List<Long>> = runCatching {
        val res = users.mapNotNull { it.chatId }
        if (res.isEmpty()) throw Exception("No chats")
        res
    }

    override fun getGuestsChatIds(): Result<List<Long>> = runCatching {
        val res = users
            .filter { it.eventStatus == Status.APPROVED }
            .mapNotNull { it.chatId }
        if (res.isEmpty()) throw Exception("No guests found")
        res
    }

    override fun create(newUser: UserInfo) {
        users.add(newUser)
    }

    override fun update(user: UserInfo) {
        val index = users.indexOfFirst { it.id == user.id }
        if (index != -1) {
            users[index] = user
        }
    }

    override fun getStatuses(offset: Long?, limit: Int?): List<UserStatus> {
        return users.mapNotNull { it.toUserStatus() }
    }
}
