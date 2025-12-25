package wedbot.domain.usecase

import wedbot.BotConstants
import wedbot.domain.policy.canDownloadCalendar
import wedbot.domain.repository.TextRepository
import wedbot.domain.repository.UserRepository
import java.io.File

class CalendarUseCase(
    private val userRepository: UserRepository,
    private val textRepository: TextRepository
) {
    sealed class Result {
        data class DocumentInfo(
            val path: File,
            val caption: String
        ) : Result()
        data class Error(val msg: String) : Result()
    }

    operator fun invoke(chatId: Long): Result {
        val user = userRepository.getByChatId(chatId).getOrElse {
            return Result.Error(textRepository.userNotFound())
        }
        return if (user.canDownloadCalendar()) {
            try {
                val file = File(BotConstants.calendarFilePath)
                Result.DocumentInfo(file, textRepository.calendarMessage())
            } catch (_: Throwable) {
                Result.Error(textRepository.internalError())
            }
        } else {
            Result.Error(textRepository.weakRights())
        }
    }
}