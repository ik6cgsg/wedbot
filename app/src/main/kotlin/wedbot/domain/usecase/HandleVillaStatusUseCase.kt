package wedbot.domain.usecase

import wedbot.BotConstants
import wedbot.domain.entity.Status
import wedbot.domain.repository.TextRepository
import wedbot.domain.repository.UserRepository
import wedbot.presentation.util.QueryData

class HandleVillaStatusUseCase(
    private val userRepository: UserRepository,
    private val textRepository: TextRepository
) {
    enum class QueryCommand(val id: String) {
        ACCEPT("villa_status_accept"),
        REJECT("villa_status_reject"),
        THINK("villa_status_think")
    }

    sealed class Input {
        object Ping : Input()
        data class Query(val value: QueryCommand) : Input()
    }

    sealed class Result {
        data class Edit(val text: String) : Result()
        data class Send(
            val text: String,
            val buttonList: List<QueryData> = listOf()
        ) : Result()
        data class DeleteWithAlert(val alert: String) : Result()
        data class Error(val msg: String) : Result()
    }

    private val queryToLabel = mapOf(
        QueryCommand.ACCEPT.id to textRepository.villaStatusAcceptButton(),
        QueryCommand.REJECT.id to textRepository.villaStatusRejectButton(),
        QueryCommand.THINK.id to textRepository.villaStatusThinkButton()
    )

    operator fun invoke(chatId: Long, input: Input): Result {
        return when (input) {
            Input.Ping -> handlePing(chatId)
            is Input.Query -> handleQuery(chatId, input.value)
        }
    }

    private fun handleQuery(chatId: Long, query: QueryCommand): Result {
        val user = userRepository.getByChatId(chatId).getOrElse {
            return Result.Error(textRepository.userNotFound())
        }
        if (user.villaStatus != Status.THINKING) {
            return Result.Error(textRepository.internalError())
        }
        return when (query) {
            QueryCommand.ACCEPT -> {
                userRepository.update(user.copy(villaStatus = Status.APPROVED))
                Result.Edit(textRepository.villaStatusAccepted())
            }
            QueryCommand.REJECT -> {
                userRepository.update(user.copy(villaStatus = Status.SLEEVE))
                Result.Edit(textRepository.villaStatusRejected())
            }
            QueryCommand.THINK -> {
                Result.DeleteWithAlert(textRepository.statusThinkAgain(BotConstants.villaStatusDeadline))
            }
        }
    }

    private fun handlePing(chatId: Long): Result {
        val user = userRepository.getByChatId(chatId).getOrElse {
            return Result.Error(textRepository.userNotFound())
        }
        return when (user.villaStatus) {
            Status.APPROVED -> Result.Send(textRepository.villaStatusPingApproved())
            Status.SLEEVE -> Result.Send(textRepository.villaStatusPingRejected())
            Status.THINKING -> {
                val markup = queryToLabel.map { QueryData(it.value, it.key) }
                Result.Send(textRepository.villaStatusPingThinking(BotConstants.villaStatusDeadline), markup)
            }
        }
    }
}