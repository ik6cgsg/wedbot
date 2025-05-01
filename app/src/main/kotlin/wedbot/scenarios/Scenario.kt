package wedbot

import com.github.kotlintelegrambot.*
import com.github.kotlintelegrambot.dispatcher.*
import com.github.kotlintelegrambot.entities.*
import com.github.kotlintelegrambot.types.TelegramBotResult

object UserMessageCommon {
    const val internalError = "Упс, что-то пошло не так, попробуй перезапустить бота (/start)"
}

abstract class Scenario(
    val bot: Bot,
    val dbUtils: DBUtils
) {
    val chatToMsgId = mutableMapOf<Long, Long>()

    open fun handleCommand(msg: Message) {
        sendInternalError(msg.chat.id)
    }

    open fun handleQuery(query: CallbackQuery) {
        query.message?.chat?.id?.let {
            sendInternalError(it)
        }
    }

    open fun handleText(text: String, chatId: Long) {
        sendInternalError(chatId)
    }

    fun sendInternalError(chatId: Long) {
        bot.sendMessage(
            chatId = ChatId.fromId(chatId),
            text = UserMessageCommon.internalError,
            replyMarkup = ReplyKeyboardRemove()
        )
    }

    fun getUserIfAuthorized(chatId: Long): UserInfo? {
        var userInfo = dbUtils.getUserByChatId(chatId)
        if (userInfo == null) {
            sendInternalError(chatId)
        }
        return userInfo
    }

    fun chatIsAuthorized(chatId: Long): Boolean {
        return getUserIfAuthorized(chatId) != null
    }

    fun queueMessageToRm(chatId: Long, tgRes: TelegramBotResult<Message>) {
        tgRes.fold(ifSuccess = { msg ->
            chatToMsgId[chatId] = msg.messageId
        }, ifError = {})
    }

    fun rmLastMessage(chatId: Long) {
        chatToMsgId[chatId]?.let { msgId ->
            bot.deleteMessage(ChatId.fromId(chatId), msgId)
            chatToMsgId.remove(chatId)
        }
    }
}
