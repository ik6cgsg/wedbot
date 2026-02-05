package wedbot.domain.usecase

import wedbot.BotConstants
import wedbot.domain.entity.TransferStatus
import wedbot.domain.repository.TextRepository
import wedbot.domain.repository.UserRepository
import wedbot.presentation.util.QueryData

class HandleTransferStatusUseCase(
    private val userRepository: UserRepository,
    private val textRepository: TextRepository
) {
    sealed class Input {
        object Ping : Input()
        data class Query(val value: String) : Input()
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

    private val callbackPrefix = "transfer_status_"

    val callbacks = TransferStatus.entries.map { it.callbackData }

    operator fun invoke(chatId: Long, input: Input): Result {
        return when (input) {
            Input.Ping -> handlePing(chatId)
            is Input.Query -> {
                val status = input.value.transferStatus
                    ?: return Result.Error(textRepository.internalError())
                handleStatus(chatId, status)
            }
        }
    }

    private fun handleStatus(chatId: Long, status: TransferStatus): Result {
        val user = userRepository.getByChatId(chatId).getOrElse {
            return Result.Error(textRepository.userNotFound())
        }
        if (user.transferStatus != TransferStatus.THINKING) {
            return Result.Error(textRepository.internalError())
        }
        return when (status) {
            TransferStatus.THINKING -> {
                Result.DeleteWithAlert(textRepository.statusThinkAgain(BotConstants.transferStatusDeadline))
            }
            TransferStatus.NEED -> {
                userRepository.update(user.copy(transferStatus = TransferStatus.NEED))
                Result.Edit(textRepository.transferStatusNeedChoice())
            }
            TransferStatus.SELF_HANDLE -> {
                userRepository.update(user.copy(transferStatus = TransferStatus.SELF_HANDLE))
                Result.Edit(textRepository.transferStatusSelfHandleChoice())
            }
            TransferStatus.SOCIAL_LEGEND -> {
                userRepository.update(user.copy(transferStatus = TransferStatus.SOCIAL_LEGEND))
                Result.Edit(textRepository.transferStatusSocialLegendChoice())
            }
        }
    }

    private fun handlePing(chatId: Long): Result {
        val user = userRepository.getByChatId(chatId).getOrElse {
            return Result.Error(textRepository.userNotFound())
        }
        return when (user.transferStatus) {
            TransferStatus.THINKING -> {
                val markup = TransferStatus.entries.map { QueryData(it.label, it.callbackData) }
                Result.Send(textRepository.transferStatusPingThinking(BotConstants.transferStatusDeadline), markup)
            }
            TransferStatus.NEED -> Result.Send(textRepository.transferStatusPingNeed())
            TransferStatus.SELF_HANDLE -> Result.Send(textRepository.transferStatusPingSelfHandle())
            TransferStatus.SOCIAL_LEGEND -> Result.Send(textRepository.transferStatusPingSocialLegend())
        }
    }

    private val TransferStatus.label: String 
        get() = when (this) {
            TransferStatus.THINKING -> textRepository.transferStatusThinkingButton()
            TransferStatus.NEED -> textRepository.transferStatusNeedButton()
            TransferStatus.SELF_HANDLE -> textRepository.transferStatusSelfHandleButton()
            TransferStatus.SOCIAL_LEGEND -> textRepository.transferStatusSocialLegendButton()
        }

    private val TransferStatus.callbackData: String
        get() = callbackPrefix + this.name.lowercase()

    private val String.transferStatus: TransferStatus?
        get() = TransferStatus.entries.find { it.callbackData == this }
}