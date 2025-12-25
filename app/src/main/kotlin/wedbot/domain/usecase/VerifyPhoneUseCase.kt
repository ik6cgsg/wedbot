package wedbot.domain.usecase

import wedbot.domain.repository.TextRepository
import wedbot.domain.repository.UserRepository

class VerifyPhoneUseCase(
    private val userRepository: UserRepository,
    private val textRepository: TextRepository
) {
    sealed class Result {
        data class UserFound(val greeting: String) : Result()
        data class Error(val msg: String) : Result()
    }

    operator fun invoke(chatId: Long, phone: String, username: String?): Result {
        val cleanPhone = phone.replace("+", "")
        val user = userRepository.getByPhone(cleanPhone).getOrNull()
        return if (user != null) { // ok, no username (or updated one?), but found by phone
            if (user.chatId == null) {
                val updatedUser = user.copy(chatId = chatId, username = username)
                userRepository.update(updatedUser)
                Result.UserFound(textRepository.generateGreeting(updatedUser.name))
            } else { // [hack] user sent another's guest number
                Result.Error(textRepository.alreadyRegistered())
            }
        } else {
            Result.Error(textRepository.userNotFound())
        }
    }
}
