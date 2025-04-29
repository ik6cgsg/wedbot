package wedbot

import com.github.kotlintelegrambot.*
import com.github.kotlintelegrambot.dispatcher.*
import com.github.kotlintelegrambot.entities.*

object UserMessageCommon {
    const val internalError = "Упс, что-то пошло не так, попробуй перезапустить бота (/start)"
}

abstract class Scenario(
    val bot: Bot,
    val dbUtils: DBUtils
) {
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

    fun test() {
        val chatId: Long = 11
        val msgId: Long = 11
        bot.deleteMessage(ChatId.fromId(chatId), msgId)
    }
}
