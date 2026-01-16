package wedbot.domain.usecase

import wedbot.BotConstants
import wedbot.domain.policy.canViewDressCode
import wedbot.domain.repository.TextRepository
import wedbot.domain.repository.UserRepository
import java.io.File

class DressCodeUseCase(
    private val userRepository: UserRepository,
    private val textRepository: TextRepository
) {
    sealed class Result {
        data class DocumentInfo(
            val path: File
        ) : Result()
        data class Error(val msg: String) : Result()
    }

    operator fun invoke(chatId: Long): Result {
        val user = userRepository.getByChatId(chatId).getOrElse {
            return Result.Error(textRepository.userNotFound())
        }
        return if (user.canViewDressCode()) {
            try {
                val file = File(BotConstants.dressCodeFilePath)
                Result.DocumentInfo(file)
            } catch (_: Throwable) {
                Result.Error(textRepository.internalError())
            }
        } else {
            Result.Error(textRepository.weakRights())
        }
    }
}