package wedbot.domain.usecase

import wedbot.domain.entity.UserStatus
import wedbot.domain.entity.toUserStatus
import wedbot.domain.repository.TextRepository
import wedbot.domain.repository.UserRepository

class StartUseCase(
    private val userRepository: UserRepository,
    private val textRepository: TextRepository
) {
    sealed class Result {
        data class UserFound(
            val greeting: String,
            val status: UserStatus
        ) : Result()
        data class NeedPhoneCheck(val error: String, val label: String) : Result()
        data class Error(val msg: String) : Result()
    }

    val commandName = "start"

    operator fun invoke(chatId: Long, username: String?): Result {
        val internalError = Result.Error(textRepository.internalError())
        var userInfo = userRepository.getByChatId(chatId).getOrNull()
        userInfo?.let { // already chatted, send greeting
            return Result.UserFound(
                textRepository.generateGreeting(it.name),
                it.toUserStatus() ?: return internalError
            )
        }
        if (username == null) { // no chat & no tg username -> need phone check
            return Result.NeedPhoneCheck(
                textRepository.shareContactError(),
                textRepository.shareContactLabel()
            )
        }
        userInfo = userRepository.getByUsername(username).getOrNull()
        return if (userInfo != null) { // user added by tg username
            val updatedUser = userInfo.copy(chatId = chatId)
            userRepository.update(updatedUser)
            Result.UserFound(
                textRepository.generateGreeting(userInfo.name),
                updatedUser.toUserStatus() ?: return internalError
            )
        } else { // user added by phone mb?
            return Result.NeedPhoneCheck(
                textRepository.shareContactError(),
                textRepository.shareContactLabel()
            )
        }
    }
}
