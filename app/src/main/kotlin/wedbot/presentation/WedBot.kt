package wedbot.presentation

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.contact
import com.github.kotlintelegrambot.dispatcher.telegramError
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.ReplyKeyboardRemove
import com.github.kotlintelegrambot.entities.ReplyMarkup
import com.github.kotlintelegrambot.entities.TelegramFile
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import com.github.kotlintelegrambot.logging.LogLevel
import com.github.kotlintelegrambot.webhook
import wedbot.CertificateUtils
import wedbot.SystemProperties
import wedbot.domain.usecase.StartUseCase
import wedbot.domain.usecase.VerifyPhoneUseCase

class WedBot(
    private val startUseCase: StartUseCase,
    private val verifyPhoneUseCase: VerifyPhoneUseCase
) {
    private val bot: Bot

    init {
        bot = bot {
            if (SystemProperties.loggerOn) logLevel = LogLevel.All()
            token = SystemProperties.botToken
            if (SystemProperties.useWebhook) webhook {
                url = "https://${SystemProperties.botHost}:8443/${SystemProperties.botToken}"
                certificate = TelegramFile.ByFile(CertificateUtils.certPathFile)
                maxConnections = 20
                allowedUpdates = listOf("message", "callback_query")
            }
            dispatch(dispatcher())
        }
    }

    fun dispatcher(): (Dispatcher.() -> Unit) = {
        command("start") {
            val chatId = message.chat.id
            val res = startUseCase(chatId, message.from?.username)
            when (res) {
                is StartUseCase.Result.UserFound -> sendMessage(
                    chatId, res.greeting
                )
                is StartUseCase.Result.NeedPhoneCheck -> sendMessage(
                     chatId, res.error,
                    shareContactMarkup(res.label)
                )
            }
        }
        contact {
            val chatId = message.chat.id
            val res = verifyPhoneUseCase(chatId, contact.phoneNumber, message.from?.username)
            when (res) {
                is VerifyPhoneUseCase.Result.UserFound -> sendMessage(chatId, res.greeting)
                is VerifyPhoneUseCase.Result.AlreadyUsed -> sendMessage(chatId, res.error)
                // TODO: ban?
                is VerifyPhoneUseCase.Result.NotFound -> sendMessage(chatId, res.error)
            }
        }
//        command(Command.START.cmd) {
//            val args = message.text?.split(" ")
//            if (args?.size == 2) {
//                scenarioInvite.handleInvitedUserStart(message)
//            } else {
//                scenarioAuth.handleCommand(message)
//            }
//        }
//        command(Command.INVITE_GUEST.cmd) {
//            scenarioInvite.handleCommand(message)
//        }
//        command(Command.CHANGE_STATUS.cmd) {
//            scenarioStatus.handleCommand(message)
//        }
//        command(Command.SAVE_CALENDAR.cmd) {
//            scenarioCalendar.handleCommand(message)
//        }
//        contact {
//            scenarioAuth.handleContact(message.chat.id, contact.phoneNumber, message.from?.username)
//        }
//        callbackQuery {
//            if (callbackQuery.data.startsWith(Command.CHANGE_STATUS.cmd)) {
//                scenarioStatus.handleQuery(callbackQuery)
//            } else if (callbackQuery.data.startsWith(Command.INVITE_GUEST.cmd)) {
//                scenarioInvite.handleQuery(callbackQuery)
//            }  else if (callbackQuery.data.startsWith(TextCommandAdmin.prefix)) {
//                scenarioAdmin.handleQuery(callbackQuery)
//            }
//        }
//        message(Filter.Sticker) {
//            bot.sendMessage(ChatId.Companion.fromId(message.chat.id), UserMessageEaster.sticker)
//        }
//        text {
//            if (text.startsWith(TextCommandAdmin.prefix)) {
//                scenarioAdmin.handleText(text, message.chat.id)
//            } else {
//                scenarioEaster.handleText(text, message.chat.id)
//            }
//        }
        telegramError {
            println(error.getErrorMessage())
        }
    }

    fun start() {
        if (SystemProperties.useWebhook) {
            bot.startWebhook()
        } else {
            bot.deleteWebhook(true)
            bot.startPolling()
        }
    }

    suspend fun process(message: String) {
        bot.processUpdate(message)
    }

    private fun sendMessage(
        chatId: Long,
        text: String,
        markup: ReplyMarkup = ReplyKeyboardRemove()
    ) {
        val chatIdTg = ChatId.fromId(chatId)
        bot.sendMessage(
            chatId = chatIdTg,
            text = text,
            parseMode = ParseMode.MARKDOWN,
            replyMarkup = markup
        )
    }

    private fun shareContactMarkup(label: String) =
        KeyboardReplyMarkup(keyboard = shareContactButton(label), resizeKeyboard = true)

    private fun shareContactButton(label: String): List<List<KeyboardButton>> {
        return listOf(
            listOf(KeyboardButton(label, requestContact = true)),
        )
    }
}