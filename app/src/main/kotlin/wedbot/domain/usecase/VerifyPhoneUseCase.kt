package wedbot.domain.usecase

import wedbot.domain.entity.UserStatus
import wedbot.domain.entity.toUserStatus
import wedbot.domain.repository.TextRepository
import wedbot.domain.repository.UserRepository

class VerifyPhoneUseCase(
    private val userRepository: UserRepository,
    private val textRepository: TextRepository
) {
    sealed class Result {
        data class UserFound(
            val greeting: String,
            val status: UserStatus
        ) : Result()
        data class Error(val msg: String) : Result()
    }

    data class Contact(val phone: String, val userId: Long? = null)
    data class MessageFrom(val username: String? = null, val userId: Long? = null)

    operator fun invoke(chatId: Long, contact: Contact, messageFrom: MessageFrom): Result {
        val cleanPhone = contact.phone.replace("+", "")
        val user = userRepository.getByPhone(cleanPhone).getOrNull()
        return when {
            contact.userId == null || contact.userId != messageFrom.userId -> { // [hack] contact from other telegram ID
                Result.Error(textRepository.shareOwnContactOnlyError())
            }
            user != null -> { // ok, no username (or updated one?), but found by phone
                if (user.chatId == null) {
                    val updatedUser = user.copy(chatId = chatId, username = messageFrom.username)
                    userRepository.update(updatedUser)
                    Result.UserFound(
                        textRepository.generateGreeting(updatedUser.name),
                        updatedUser.toUserStatus() ?: return Result.Error(textRepository.internalError())
                    )
                } else { // [hack] user sent another's guest number
                    Result.Error(textRepository.alreadyRegistered())
                }
            }
            else -> Result.Error(textRepository.userNotFound())
        }
    }
}
