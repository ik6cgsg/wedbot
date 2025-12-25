package wedbot.domain.usecase

import wedbot.domain.entity.UserInfo
import wedbot.domain.policy.canChangeStatus
import wedbot.domain.policy.canContactOrganizers
import wedbot.domain.policy.canDownloadCalendar
import wedbot.domain.policy.canViewAdminPanel
import wedbot.domain.policy.canViewInfo
import wedbot.domain.policy.canViewLocation
import wedbot.domain.repository.TextRepository
import wedbot.domain.repository.UserRepository

class MenuUseCase(
    private val userRepository: UserRepository,
    private val textRepository: TextRepository
) {
    sealed class Result {
        data class Markup(
            val message: String,
            val buttons: List<List<String>>
        ) : Result()
        data class Error(val msg: String) : Result()
    }

    enum class Button(val label: String) {
        INFO("Информация о празднике"),
        ICS("Календарик"),
        LOCATION("Локация"),
        EVENT_STATUS("Установить статус посещения мероприятия"),
        HELP("Связаться с организаторами"),
        STATUS_TABLE("Таблица со статусами"),
        PING_GUESTS("Отправить гостям сообщение")
    }

    val commandName = "menu"

    operator fun invoke(chatId: Long): Result {
        val user = userRepository.getByChatId(chatId).getOrElse {
            return Result.Error(textRepository.userNotFound())
        }
        val keyboard = mutableListOf<List<String>>()
        if (user.canViewInfo()) {
            keyboard.add(listOf(Button.INFO.label))
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
        return Result.Markup(
            textRepository.menuMessage(),
            keyboard
        )
    }
}
