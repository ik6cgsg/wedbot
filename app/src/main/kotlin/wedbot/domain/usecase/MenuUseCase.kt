package wedbot.domain.usecase

import wedbot.domain.entity.Status
import wedbot.domain.policy.canChangeStatus
import wedbot.domain.policy.canContactOrganizers
import wedbot.domain.policy.canDownloadCalendar
import wedbot.domain.policy.canViewAdminPanel
import wedbot.domain.policy.canViewDressCode
import wedbot.domain.policy.canViewInfo
import wedbot.domain.policy.canViewLocation
import wedbot.domain.repository.TextRepository
import wedbot.domain.repository.UserRepository

class MenuUseCase(
    private val userRepository: UserRepository,
    private val textRepository: TextRepository
) {
    companion object {
        const val COMMAND_NAME = "menu"
    }

    sealed class Result {
        data class Markup(
            val message: String,
            val buttons: List<List<String>>
        ) : Result()
        data class Error(val msg: String) : Result()
    }

    enum class Button {
        INFO,
        ICS,
        LOCATION,
        EVENT_STATUS,
        HELP,
        STATUS_TABLE,
        PING_GUESTS,
        DRESS_CODE
    }

    val buttonToLabel = mapOf(
        Button.INFO to textRepository.menuButtonInfo(),
        Button.ICS to textRepository.menuButtonIcs(),
        Button.LOCATION to textRepository.menuButtonLocation(),
        Button.EVENT_STATUS to textRepository.menuButtonEventStatus(),
        Button.HELP to textRepository.menuButtonHelp(),
        Button.STATUS_TABLE to textRepository.menuButtonStatusTable(),
        Button.PING_GUESTS to textRepository.menuButtonPingGuests(),
        Button.DRESS_CODE to textRepository.menuButtonDressCode()
    )

    private val Button.label: String
        get() = buttonToLabel[this] ?: ""

    operator fun invoke(chatId: Long): Result {
        val user = userRepository.getByChatId(chatId).getOrElse {
            return Result.Error(textRepository.userNotFound())
        }
        val keyboard = mutableListOf<List<String>>()
        if (user.canViewInfo()) {
            keyboard.add(listOf(Button.INFO.label))
        }
        if (user.canViewDressCode()) {
            keyboard.add(listOf(Button.DRESS_CODE.label))
        }
        if (user.canViewAdminPanel()) {
            keyboard.add(listOf(Button.STATUS_TABLE.label))
            keyboard.add(listOf(Button.PING_GUESTS.label))
        }
        val logisticRow = mutableListOf<String>()
        if (user.canDownloadCalendar()) {
            logisticRow.add(Button.ICS.label)
        }
        if (user.canViewLocation()) {
            logisticRow.add(Button.LOCATION.label)
        }
        if (logisticRow.isNotEmpty()) {
            keyboard.add(logisticRow)
        }
        if (user.canChangeStatus()) {
            keyboard.add(listOf(Button.EVENT_STATUS.label))
        }
        if (user.canContactOrganizers()) {
            keyboard.add(listOf(Button.HELP.label))
        }
        if (keyboard.isEmpty()) {
            return Result.Error(textRepository.weakRights())
        }
        val message = if (user.eventStatus == Status.SLEEVE) {
            textRepository.totalSleeve()
        } else {
            textRepository.menuMessage()
        }
        return Result.Markup(message, keyboard)
    }
}
