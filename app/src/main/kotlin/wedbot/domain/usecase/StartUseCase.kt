package wedbot.domain.usecase

import wedbot.domain.repository.TextRepository
import wedbot.domain.repository.UserRepository

class StartUseCase(
    private val userRepository: UserRepository,
    private val textRepository: TextRepository
) {
    sealed class Result {
        data class UserFound(val greeting: String) : Result()
        data class NeedPhoneCheck(val error: String, val label: String) : Result()
    }

    operator fun invoke(chatId: Long, username: String?): Result {
        var userInfo = userRepository.getByChatId(chatId).getOrNull()
        if (userInfo != null) { // already chatted, send greeting
            return Result.UserFound(textRepository.generateGreeting(userInfo.name))
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
            Result.UserFound(textRepository.generateGreeting(userInfo.name))
        } else { // user added by phone mb?
            return Result.NeedPhoneCheck(
                textRepository.shareContactError(),
                textRepository.shareContactLabel()
            )
        }
    }
}
