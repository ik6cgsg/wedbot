package wedbot.domain.usecase

import wedbot.domain.entity.Status
import wedbot.domain.repository.TextRepository
import wedbot.domain.repository.UserRepository

class HandleEventStatusUseCase(
    private val userRepository: UserRepository,
    private val textRepository: TextRepository
) {
    sealed class Result {
        data class Edit(val newText: String) : Result()
        data class DeleteWithAlert(val alert: String) : Result()
        data class Error(val msg: String) : Result()
    }

    data class QueryCommand(
        val callback: String,
        val text: String,
        val status: Status
    )

    val commandName = "event_status"
    val queries = listOf(
        QueryCommand("${commandName}_accept",
            textRepository.eventStatusAcceptButton(), Status.APPROVED),
        QueryCommand("${commandName}_reject",
            textRepository.eventStatusRejectButton(), Status.SLEEVE),
        QueryCommand("${commandName}_think",
            textRepository.eventStatusThinkButton(), Status.THINKING)
    )

    operator fun invoke(chatId: Long, status: Status): Result {
        val user = userRepository.getByChatId(chatId).getOrElse {
            return Result.Error(textRepository.userNotFound())
        }
        if (user.eventStatus != Status.THINKING) {
            return Result.Error(textRepository.internalError())
        }
        return when (status) {
            Status.APPROVED -> {
                userRepository.update(user.copy(eventStatus = Status.APPROVED))
                Result.Edit(textRepository.eventStatusAccepted())
            }
            Status.SLEEVE -> {
                // TODO: delete??
                userRepository.update(user.copy(eventStatus = Status.SLEEVE))
                Result.Edit(textRepository.eventStatusRejected())
            }
            Status.THINKING -> {
                Result.DeleteWithAlert(textRepository.eventStatusThinkAgain())
            }
        }
    }
}