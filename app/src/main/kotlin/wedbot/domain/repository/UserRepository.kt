package wedbot.domain.repository

import wedbot.domain.entity.UserInfo
import wedbot.domain.entity.UserStatus

interface UserRepository {
    fun getByUsername(username: String): Result<UserInfo>
    fun getByPhone(phone: String): Result<UserInfo>
    fun getByChatId(chatId: Long): Result<UserInfo>
    
    fun getAdminChatIds(): List<Long>
    fun getAllUserChatIds(): List<Long>
    
    fun create(newUser: UserInfo)
    fun update(user: UserInfo)
    
    fun getStatuses(offset: Long?, limit: Int?): List<UserStatus>
}
