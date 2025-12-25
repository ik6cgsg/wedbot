package wedbot.domain.usecase

import wedbot.BotConstants
import wedbot.domain.policy.canViewLocation
import wedbot.domain.repository.TextRepository
import wedbot.domain.repository.UserRepository

class LocationUseCase(
    private val userRepository: UserRepository,
    private val textRepository: TextRepository
) {
    sealed class Result {
        data class Location(
            val latitude: Float,
            val longitude: Float
        ) : Result()
        data class Error(val msg: String) : Result()
    }

    operator fun invoke(chatId: Long): Result {
        val user = userRepository.getByChatId(chatId).getOrElse {
            return Result.Error(textRepository.userNotFound())
        }
        return if (user.canViewLocation()) {
            try {
                val coords = BotConstants.locationCoordinates
                    .replace(" ", "")
                    .split(",")
                Result.Location(coords[0].toFloat(), coords[1].toFloat())
            } catch (_: Throwable) {
                Result.Error(textRepository.internalError())
            }
        } else {
            Result.Error(textRepository.weakRights())
        }
    }
}