package wedbot

import java.io.File
import com.github.kotlintelegrambot.entities.TelegramFile

object UserMessage {
    private val greetingTemplate = """
    Здравствуй, дорог%s %s%s!
    Рады сообщить, что приглашаем Тебя на наш праздник, который будет состоять из двух этапов:
    1. ЗАГС (опционально)
    2. Праздник жизни (musthave)
    """
    private val statusTemplate = """
    Твой текущий статус: `%s`
    На что меняем?
    """
    private val statusResultTemplate = """
    Спасибо! Статус сменен на: `%s`!
    """
    val alreadyChatted = "А мы уже знакомы! Список команд можешь увидеть в меню ниже"
    val notFound = "Кажется, я тебя не узнал, поделись пожалуйста контактом!"
    val sticker = "WOW, крутой стикерпак 🥵"
    val invitationCaption = "Лови открытку от нас 🥺🫶"
    val calendarCaption = "Сохраняем в календарь, не стесняемся 😎"
    val internalError = "Упс, что-то пошло не так, попробуйте перезапустить бота (/start)"
    val statusMap = mapOf(
        Status.ACCEPT to "Пойду 💯", 
        Status.REJECT to "Не смогу 😭",
        Status.NOT_SURE to "Пока думаю 🤨"
    )

    fun generateGreeting(sex: Sex, name: String?, nik: String?) = greetingTemplate.format(
        if (sex == Sex.FEMALE) "ая" else "ой",
        name ?: "пользователь без тг имени",
        if (nik?.isNotBlank() == true) ", более известный как `$nik` xDD" else ""
    )

    fun generateStatus(status: Status) = statusTemplate.format(statusMap[status])
    fun generateStatusResult(status: Status) = statusResultTemplate.format(statusMap[status])
}

object KeyboardButtonText {
    val shareContact = "Поделиться контактом 🥺"
}

object CommandName {
    val start = "start"
    val saveCalendar = "ics"
    // [query] status
    val changeStatus = "status"
    val statusChangeAccept = "statusChangeAccept"
    val statusChangeReject = "statusChangeReject"
    val statusChangeNotSure = "statusChangeNotSure"
    // [query] invite
    val invite = "invite"
    val inviteUserAccept = "inviteUserAccept"
    val inviteUserReject = "inviteUserReject"
    val inviteAdminAccept = "inviteAdminAccept"
    val inviteAdminReject = "inviteAdminReject"
    // TODO: admin cmds (can it be on menu???) or just text
    val adminUserStatuses = "admin_statuses"
    val adminSendTextToAllUsers = "admin_ping_users"
    val adminRemindAcceptedUsers = "admin_remind"
}

object Document {
    val calendarEvent = TelegramFile.ByFile(File("res/wed.ics"))
    val invitationPic = TelegramFile.ByFile(File("res/zagz.png"))
}
