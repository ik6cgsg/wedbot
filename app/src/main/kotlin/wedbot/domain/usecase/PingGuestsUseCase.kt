package wedbot.domain.usecase

import wedbot.domain.policy.canViewAdminPanel
import wedbot.domain.repository.TextRepository
import wedbot.domain.repository.UserRepository

class PingGuestsUseCase(
    private val userRepository: UserRepository,
    private val textRepository: TextRepository
) {
    companion object {
        const val COMMAND_CANCEL = "ping_cancel"
    }

    sealed class CheckResult {
        data class Allowed(
            val startMessage: String,
            val prompt: String,
            val cancelButton: String
        ) : CheckResult()
        data class Error(val msg: String) : CheckResult()
    }

    fun checkRights(initiatorChatId: Long): CheckResult {
        val user = userRepository.getByChatId(initiatorChatId).getOrElse {
            return CheckResult.Error(textRepository.userNotFound())
        }
        return if (user.canViewAdminPanel()) {
            CheckResult.Allowed(
                textRepository.adminPingStarted(),
                textRepository.adminPingPrompt(),
                textRepository.adminPingCancelButton()
            )
        } else {
            CheckResult.Error(textRepository.weakRights())
        }
    }

    fun getAllChats(initiatorChatId: Long): List<Long> {
        val chatList = userRepository.getAllUserChatIds().getOrElse {
            return listOf()
        }
        return chatList
            .filter { it != initiatorChatId }
            .distinct()
    }
}
