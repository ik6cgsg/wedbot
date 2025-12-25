package wedbot.presentation

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.*
import com.github.kotlintelegrambot.entities.*
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import com.github.kotlintelegrambot.logging.LogLevel
import com.github.kotlintelegrambot.webhook
import kotlinx.coroutines.delay
import wedbot.SystemProperties
import wedbot.domain.entity.*
import wedbot.domain.repository.*
import wedbot.domain.usecase.*
import wedbot.presentation.server.CertificateUtils

class WedBot(
    private val textRepository: TextRepository,
    private val startUseCase: StartUseCase,
    private val verifyPhoneUseCase: VerifyPhoneUseCase,
    private val handleEventStatusUseCase: HandleEventStatusUseCase,
    private val menuUseCase: MenuUseCase,
    private val calendarUseCase: CalendarUseCase,
    private val locationUseCase: LocationUseCase
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
        command(startUseCase.commandName) {
            val chatId = message.chat.id
            val res = startUseCase(chatId, message.from?.username)
            when (res) {
                is StartUseCase.Result.UserFound -> {
                    userVerifiedAfterStart(chatId, res.greeting, res.status)
                }
                is StartUseCase.Result.NeedPhoneCheck -> sendMessage(
                     chatId, res.error,
                    shareContactMarkup(res.label)
                )
                is StartUseCase.Result.Error -> sendMessage(chatId, res.msg)
            }
        }
        contact {
            val chatId = message.chat.id
            val res = verifyPhoneUseCase(chatId, contact.phoneNumber, message.from?.username)
            when (res) {
                is VerifyPhoneUseCase.Result.UserFound -> sendMessage(chatId, res.greeting)
                is VerifyPhoneUseCase.Result.Error -> sendMessage(chatId, res.msg)
            }
        }
        handleEventStatusUseCase.queries.forEach { query ->
            callbackQuery(query.callback) {
                val chatId = callbackQuery.from.id
                val res = handleEventStatusUseCase.invoke(chatId, query.status)
                when (res) {
                    is HandleEventStatusUseCase.Result.DeleteWithAlert -> {
                        bot.answerCallbackQuery(
                            callbackQuery.id,
                            text = res.alert,
                            showAlert = true
                        )
                        callbackQuery.message?.messageId?.let {
                            bot.deleteMessage(ChatId.fromId(chatId), it)
                        }
                    }
                    is HandleEventStatusUseCase.Result.Edit -> bot.editMessageText(
                        chatId = ChatId.fromId(chatId),
                        messageId = callbackQuery.message?.messageId,
                        text = res.newText
                    )
                    is HandleEventStatusUseCase.Result.Error -> {
                        bot.answerCallbackQuery(
                            callbackQuery.id,
                            text = res.msg,
                            showAlert = false
                        )
                        callbackQuery.message?.messageId?.let {
                            bot.deleteMessage(ChatId.fromId(chatId), it)
                        }
                    }
                }
            }
        }
        command(menuUseCase.commandName) {
            val chatId = message.chat.id
            val res = menuUseCase.invoke(message.chat.id)
            when (res) {
                is MenuUseCase.Result.Markup -> sendMessage(
                    chatId, res.message, createReplyMarkup(res.buttons)
                )
                is MenuUseCase.Result.Error -> sendMessage(chatId, res.msg)
            }
        }
        MenuUseCase.Button.entries.forEach { button ->
            text(button.label) {
                val chatId = message.chat.id
                when (button) {
                    MenuUseCase.Button.INFO -> {
                        sendMessage(chatId, textRepository.generateGreeting(null))
                    }
                    MenuUseCase.Button.ICS -> {
                        val res = calendarUseCase.invoke(chatId)
                        when (res) {
                            is CalendarUseCase.Result.DocumentInfo -> {
                                bot.sendDocument(
                                    chatId = ChatId.fromId(chatId),
                                    document = TelegramFile.ByFile(res.path),
                                    caption = res.caption
                                )
                            }
                            is CalendarUseCase.Result.Error -> sendMessage(chatId, res.msg)
                        }
                    }
                    MenuUseCase.Button.LOCATION -> {
                        val res = locationUseCase.invoke(chatId)
                        when (res) {
                            is LocationUseCase.Result.Location ->
                                bot.sendLocation(ChatId.fromId(chatId), res.latitude, res.longitude)
                            is LocationUseCase.Result.Error -> sendMessage(chatId, res.msg)
                        }
                    }
                    MenuUseCase.Button.EVENT_STATUS -> pingEventStatusFirst(chatId)
                    MenuUseCase.Button.HELP -> sendMessage(chatId, "HELP MESSAGE")
                    MenuUseCase.Button.STATUS_TABLE -> TODO()
                    MenuUseCase.Button.PING_GUESTS -> TODO()
                }
            }
        }
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

    fun pingEventStatus(chatId: Long, name: String?) {
        sendMessage(chatId,
            textRepository.eventStatusPingDaily(name),
            createInlineMarkup(handleEventStatusUseCase.queries.map {
                InlineKeyboardButton.CallbackData(it.text, it.callback)
            })
        )
    }

    fun pingEventStatusFirst(chatId: Long) {
        sendMessage(
            chatId,
            textRepository.eventStatusPingFirst(),
            createInlineMarkup(handleEventStatusUseCase.queries.map {
                InlineKeyboardButton.CallbackData(it.text, it.callback)
            })
        )
    }

    private suspend fun userVerifiedAfterStart(chatId: Long, greeting: String, status: UserStatus) {
        sendMessage(chatId, greeting)
        delay(1000)
        if (status.eventStatus == Status.THINKING) pingEventStatusFirst(chatId)
    }

    private fun sendMessage(
        chatId: Long,
        text: String,
        markup: ReplyMarkup = ReplyKeyboardRemove()
    ) {
        val chatIdTg = ChatId.fromId(chatId)
        bot.sendChatAction(chatIdTg, ChatAction.TYPING)
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

    private fun createReplyMarkup(markup: List<List<String>>): ReplyMarkup {
        val keyboardMarkup = mutableListOf<List<KeyboardButton>>()
        markup.forEach { row ->
            keyboardMarkup.add(row.map { label -> KeyboardButton(label) })
        }
        return KeyboardReplyMarkup(keyboard = keyboardMarkup, resizeKeyboard = true)
    }

    private fun createInlineMarkup(callbacks: List<InlineKeyboardButton.CallbackData>): InlineKeyboardMarkup {
        val buttons = mutableListOf<List<InlineKeyboardButton>>()
        for (callback in callbacks) {
            buttons.add(listOf(callback))
        }
        return InlineKeyboardMarkup.create(buttons)
    }
}