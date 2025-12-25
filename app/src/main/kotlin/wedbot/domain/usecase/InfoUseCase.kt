package wedbot.domain.usecase

import wedbot.domain.policy.canViewInfo
import wedbot.domain.repository.TextRepository
import wedbot.domain.repository.UserRepository

class InfoUseCase(
    private val userRepository: UserRepository,
    private val textRepository: TextRepository
) {
    sealed class Result {
        data class Info(val text: String) : Result()
        data class Error(val msg: String) : Result()
    }

    operator fun invoke(chatId: Long): Result {
        val user = userRepository.getByChatId(chatId).getOrElse {
            return Result.Error(textRepository.userNotFound())
        }
        return if (user.canViewInfo()) {
            Result.Info(textRepository.infoMessage())
        } else {
            Result.Error(textRepository.weakRights())
        }
    }
}
