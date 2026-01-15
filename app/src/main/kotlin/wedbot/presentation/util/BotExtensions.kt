package wedbot.presentation.util

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatAction
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.ReplyKeyboardRemove
import com.github.kotlintelegrambot.entities.ReplyMarkup
import java.util.logging.Level
import java.util.logging.Logger

private val logger = Logger.getLogger("BotExtensions")

fun Bot.sendSafeMessage(
    chatId: Long,
    text: String,
    markup: ReplyMarkup = ReplyKeyboardRemove()
) {
    try {
        val chatIdTg = ChatId.fromId(chatId)
        sendChatAction(chatIdTg, ChatAction.TYPING)
        sendMessage(
            chatId = chatIdTg,
            text = text,
            parseMode = ParseMode.MARKDOWN,
            replyMarkup = markup
        )
    } catch (e: Throwable) {
        logger.log(Level.WARNING, "Failed to send message to $chatId", e)
    }
}

fun Bot.editSafeMessage(
    chatId: Long,
    messageId: Long?,
    text: String,
    markup: ReplyMarkup? = null
) {
    try {
        editMessageText(
            chatId = ChatId.fromId(chatId),
            messageId = messageId,
            text = text,
            parseMode = ParseMode.MARKDOWN,
            replyMarkup = markup
        )
    } catch (e: Throwable) {
        logger.log(Level.WARNING, "Failed to edit message $messageId in chat $chatId", e)
    }
}
