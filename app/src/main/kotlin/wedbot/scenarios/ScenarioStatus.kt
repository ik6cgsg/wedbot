package wedbot

import com.github.kotlintelegrambot.*
import com.github.kotlintelegrambot.dispatcher.*
import com.github.kotlintelegrambot.entities.*
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton

object UserMessageStatus {
    private const val statusTemplate = """
    Твой текущий статус: `%s`
    На что меняем?
    """
    private const val statusResultTemplate = """
    Спасибо! Статус сменен на: `%s`!
    """

    fun statusToMessage(status: Status): String = when(status) {
        Status.ACCEPT -> "Пойду 💯", 
        Status.REJECT -> "Не смогу 😭",
        Status.NOT_SURE -> "Пока думаю 🤨"
    }
    fun generateStatus(status: Status) = statusTemplate.format(statusToMessage(status))
    fun generateStatusResult(status: Status) = statusResultTemplate.format(statusToMessage(status))
}

object QueryStatus {
    val statusChangeAccept = "${CommandName.changeStatus}ChangeAccept"
    val statusChangeReject = "${CommandName.changeStatus}ChangeReject"
    val statusChangeNotSure = "${CommandName.changeStatus}ChangeNotSure"
}

class ScenarioStatus(
    bot: Bot,
    dbUtils: DBUtils
): Scenario(bot, dbUtils) {
    override fun handleCommand(msg: Message) {
        val chatId = msg.chat.id
        getUserIfAuthorized(chatId)?.let {
            bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = UserMessageStatus.generateStatus(it.status),
                replyMarkup = InlineKeyboardMarkup.create(inlineStatusButtons())
            )
        }
    }

    override fun handleQuery(query: CallbackQuery) {
        val chatId = query.message?.chat?.id ?: return
        when (query.data) {
            QueryStatus.statusChangeAccept -> handleChangeStatusResult(chatId, Status.ACCEPT)
            QueryStatus.statusChangeReject -> handleChangeStatusResult(chatId, Status.REJECT)
            QueryStatus.statusChangeNotSure -> handleChangeStatusResult(chatId, Status.NOT_SURE)
            else -> sendInternalError(chatId)
        }
    }

    private fun handleChangeStatusResult(chatId: Long, newStatus: Status) {
        getUserIfAuthorized(chatId)?.let {
            it.status = newStatus
            dbUtils.updateUser(it)
            bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = UserMessageStatus.generateStatusResult(newStatus)
            )
        }
    }

    private fun inlineStatusButtons(): List<List<InlineKeyboardButton>> = listOf(
        listOf(InlineKeyboardButton.CallbackData(
            UserMessageStatus.statusToMessage(Status.ACCEPT), QueryStatus.statusChangeAccept)),
        listOf(InlineKeyboardButton.CallbackData(
            UserMessageStatus.statusToMessage(Status.REJECT), QueryStatus.statusChangeReject)),
        listOf(InlineKeyboardButton.CallbackData(
            UserMessageStatus.statusToMessage(Status.NOT_SURE), QueryStatus.statusChangeNotSure))
    )
}
