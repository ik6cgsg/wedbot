package wedbot.domain.usecase

import wedbot.domain.policy.canViewAdminPanel
import wedbot.domain.repository.TextRepository
import wedbot.domain.repository.UserRepository
import wedbot.presentation.util.QueryData
import java.util.Collections

class PingGuestsUseCase(
    private val userRepository: UserRepository,
    private val textRepository: TextRepository
) {
    enum class QueryCommand(val id: String) {
        CANCEL("ping_cancel"),
        SWITCH_TO_ALL("ping_all"),
        SWITCH_TO_GUESTS("ping_guests")
    }

    sealed class Input {
        object Start : Input()
        data class Query(val value: QueryCommand) : Input()
        object SendText : Input()
    }

    sealed class Result {
        data class Start(
            val startMessage: String,
            val prompt: String,
            val buttonList: List<QueryData>
        ) : Result()
        data class UpdateQuery(
            val text: String,
            val buttonList: List<QueryData>
        ) : Result()
        data class PingRecipients(
            val chatIdList: List<Long>,
            val messagePrefix: String,
            val adminMessage: String
        ) : Result()
        data class Error(val msg: String) : Result()
    }

    private enum class PingMode { ALL, GUESTS }

    private val adminsInPingMode = Collections.synchronizedMap(mutableMapOf<Long, PingMode>())

    fun invoke(chatId: Long, input: Input): Result {
        return when (input) {
            Input.Start -> start(chatId)
            is Input.Query -> handleQuery(chatId, input.value)
            Input.SendText -> getRecipients(chatId)
        }
    }

    fun isUserInPingMode(chatId: Long): Boolean {
        return adminsInPingMode.contains(chatId)
    }

    private fun start(chatId: Long): Result {
        val user = userRepository.getByChatId(chatId).getOrElse {
            return Result.Error(textRepository.userNotFound())
        }
        return if (user.canViewAdminPanel()) {
            adminsInPingMode[chatId] = PingMode.ALL
            Result.Start(
                textRepository.adminPingStarted(),
                textRepository.adminPingAllPrompt(),
                listOf(
                    QueryData(textRepository.adminPingGuestsButton(), QueryCommand.SWITCH_TO_GUESTS.id),
                    QueryData(textRepository.adminPingCancelButton(), QueryCommand.CANCEL.id)
                )
            )
        } else {
            Result.Error(textRepository.weakRights())
        }
    }

    private fun handleQuery(chatId: Long, query: QueryCommand): Result {
        if (!adminsInPingMode.contains(chatId)) {
            return Result.Error(textRepository.userNotFound())
        }
        return when (query) {
            QueryCommand.CANCEL -> {
                adminsInPingMode.remove(chatId)
                Result.UpdateQuery(
                    textRepository.adminPingCancel(), listOf()
                )
            }
            QueryCommand.SWITCH_TO_ALL -> {
                adminsInPingMode[chatId] = PingMode.ALL
                Result.UpdateQuery(
                    textRepository.adminPingAllPrompt(), listOf(
                        QueryData(textRepository.adminPingGuestsButton(), QueryCommand.SWITCH_TO_GUESTS.id),
                        QueryData(textRepository.adminPingCancelButton(), QueryCommand.CANCEL.id)
                    )
                )
            }
            QueryCommand.SWITCH_TO_GUESTS -> {
                adminsInPingMode[chatId] = PingMode.GUESTS
                Result.UpdateQuery(
                    textRepository.adminPingGuestsPrompt(), listOf(
                        QueryData(textRepository.adminPingAllButton(), QueryCommand.SWITCH_TO_ALL.id),
                        QueryData(textRepository.adminPingCancelButton(), QueryCommand.CANCEL.id)
                    )
                )
            }
        }
    }

    private fun getRecipients(chatId: Long): Result {
        val pingMode = adminsInPingMode[chatId]
        adminsInPingMode.remove(chatId)
        val prefix = textRepository.adminMessageHeader() + "\n\n"
        val admin = textRepository.adminPingSucceed()
        return when (pingMode) {
            PingMode.ALL -> Result.PingRecipients(
                getAllChats(chatId), prefix, admin
            )
            PingMode.GUESTS -> Result.PingRecipients(
                getGuestsChats(chatId), prefix, admin
            )
            null -> Result.Error(textRepository.internalError())
        }
    }

    private fun getAllChats(initiatorChatId: Long): List<Long> {
        val chatList = userRepository.getAllUserChatIds().getOrElse {
            return listOf()
        }
        return chatList
            .filter { it != initiatorChatId }
            .distinct()
    }

    private fun getGuestsChats(initiatorChatId: Long): List<Long> {
        val chatList = userRepository.getGuestsChatIds().getOrElse {
            return listOf()
        }
        return chatList
            .filter { it != initiatorChatId }
            .distinct()
    }
}
