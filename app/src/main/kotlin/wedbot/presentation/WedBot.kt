package wedbot.presentation

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.*
import com.github.kotlintelegrambot.entities.*
import com.github.kotlintelegrambot.entities.dice.DiceEmoji
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
import java.util.Collections

class WedBot(
    private val textRepository: TextRepository,
    private val startUseCase: StartUseCase,
    private val verifyPhoneUseCase: VerifyPhoneUseCase,
    private val handleEventStatusUseCase: HandleEventStatusUseCase,
    private val menuUseCase: MenuUseCase,
    private val calendarUseCase: CalendarUseCase,
    private val locationUseCase: LocationUseCase,
    private val statusTableUseCase: StatusTableUseCase,
    private val infoUseCase: InfoUseCase,
    private val pingGuestsUseCase: PingGuestsUseCase,
    private val easterUseCase: EasterUseCase
) {
    private val bot: Bot
    private val adminsInPingMode = Collections.synchronizedSet(mutableSetOf<Long>())

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
        pingDispatcher().invoke(this)
        authDispatcher().invoke(this)
        eventStatusDispatcher().invoke(this)
        menuDispatcher().invoke(this)
        statusTableDispatcher().invoke(this)
        easterDispatcher().invoke(this)
        telegramError {
            println(error.getErrorMessage())
        }
    }

    // MARK: public API

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

    // MARK: authorization

    fun authDispatcher(): (Dispatcher.() -> Unit) = {
        command(StartUseCase.COMMAND_NAME) {
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
                is VerifyPhoneUseCase.Result.UserFound -> {
                    userVerifiedAfterStart(chatId, res.greeting, res.status)
                }
                is VerifyPhoneUseCase.Result.Error -> sendMessage(chatId, res.msg)
            }
        }
    }

    private suspend fun userVerifiedAfterStart(chatId: Long, greeting: String, status: UserStatus) {
        sendMessage(chatId, greeting)
        if (status.eventStatus == Status.THINKING) {
            bot.sendChatAction(ChatId.fromId(chatId), ChatAction.TYPING)
            delay(1000)
            pingEventStatusFirst(chatId)
        }
    }

    private fun pingEventStatusFirst(chatId: Long) {
        sendMessage(
            chatId,
            textRepository.eventStatusPingFirst(),
            createInlineMarkup(handleEventStatusUseCase.queries.map {
                InlineKeyboardButton.CallbackData(it.text, it.callback)
            })
        )
    }

    // MARK: eventStatus dispatcher

    fun eventStatusDispatcher(): (Dispatcher.() -> Unit) = {
        handleEventStatusUseCase.queries.forEach { query ->
            callbackQuery(query.callback) {
                val chatId = callbackQuery.from.id
                val res = handleEventStatusUseCase(chatId, query.status)
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
                    is HandleEventStatusUseCase.Result.Edit -> {
                        bot.editMessageText(
                            chatId = ChatId.fromId(chatId),
                            messageId = callbackQuery.message?.messageId,
                            text = res.newText
                        )
                        sendMessage(chatId, textRepository.menuUpdated())
                    }
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
    }

    // MARK: menu

    fun menuDispatcher(): (Dispatcher.() -> Unit) = {
        command(MenuUseCase.COMMAND_NAME) {
            val chatId = message.chat.id
            val res = menuUseCase(message.chat.id)
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
                if (adminsInPingMode.contains(chatId)) return@text
                when (button) {
                    MenuUseCase.Button.INFO -> showInfo(chatId)
                    MenuUseCase.Button.ICS -> showCalendar(chatId)
                    MenuUseCase.Button.LOCATION -> showLocation(chatId)
                    MenuUseCase.Button.EVENT_STATUS -> pingEventStatusFirst(chatId)
                    MenuUseCase.Button.HELP -> sendMessage(chatId, textRepository.helpMessage())
                    MenuUseCase.Button.STATUS_TABLE -> showStatusTable(chatId)
                    MenuUseCase.Button.PING_GUESTS -> startPingGuestsFlow(chatId)
                }
            }
        }
    }

    private fun showInfo(chatId: Long) {
        val res = infoUseCase(chatId)
        when (res) {
            is InfoUseCase.Result.Info -> sendMessage(chatId, res.text)
            is InfoUseCase.Result.Error -> sendMessage(chatId, res.msg)
        }
    }

    // MARK: ping guests

    private fun startPingGuestsFlow(chatId: Long) {
        val res = pingGuestsUseCase.checkRights(chatId)
        when (res) {
            is PingGuestsUseCase.CheckResult.Allowed -> {
                adminsInPingMode.add(chatId)
                sendMessage(chatId, res.prompt)
            }
            is PingGuestsUseCase.CheckResult.Error -> sendMessage(chatId, res.msg)
        }
    }

    private fun pingDispatcher(): (Dispatcher.() -> Unit) = {
        command(PingGuestsUseCase.COMMAND_CANCEL) {
            val chatId = message.chat.id
            if (adminsInPingMode.contains(chatId)) {
                adminsInPingMode.remove(chatId)
                sendMessage(chatId, textRepository.adminPingCancel())
            }
        }
        text {
            val msg = message.text
            if (msg.isNullOrEmpty() || msg.startsWith("/")) return@text
            val chatId = message.chat.id
            if (!adminsInPingMode.contains(chatId)) return@text
            val chats = pingGuestsUseCase.getAllChats(chatId)
            chats.forEach { id ->
                // TODO: admin header
                sendMessage(id, msg)
            }
            adminsInPingMode.remove(chatId)
            sendMessage(chatId, textRepository.adminPingSucceed())
        }
    }

    // MARK: geo data info

    private fun showLocation(chatId: Long) {
        val res = locationUseCase(chatId)
        when (res) {
            is LocationUseCase.Result.Location -> bot.sendLocation(
                ChatId.fromId(chatId),
                res.latitude, res.longitude
            )
            is LocationUseCase.Result.Error -> sendMessage(chatId, res.msg)
        }
    }

    private fun showCalendar(chatId: Long) {
        val res = calendarUseCase(chatId)
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

    // MARK: status table

    private fun statusTableDispatcher(): (Dispatcher.() -> Unit) = {
        callbackQuery {
            if (callbackQuery.data.startsWith(StatusTableUseCase.CALLBACK_PREFIX)) {
                val page = callbackQuery.data.removePrefix(StatusTableUseCase.CALLBACK_PREFIX).toIntOrNull() ?: 0
                val chatId = callbackQuery.from.id
                val res = statusTableUseCase(chatId, page)
                when (res) {
                    is StatusTableUseCase.Result.Table -> {
                        val markup = createPaginationMarkup(res.currentPage, res.hasPreviousPage, res.hasNextPage)
                        bot.editMessageText(
                            chatId = ChatId.fromId(chatId),
                            messageId = callbackQuery.message?.messageId,
                            text = res.tableText,
                            parseMode = ParseMode.MARKDOWN,
                            replyMarkup = markup
                        )
                    }
                    is StatusTableUseCase.Result.Error -> {
                        bot.answerCallbackQuery(
                            callbackQuery.id, res.msg, false
                        )
                        callbackQuery.message?.messageId?.let {
                            bot.deleteMessage(ChatId.fromId(chatId), it)
                        }
                    }
                }
            }
        }
    }

    private fun showStatusTable(chatId: Long) {
        val res = statusTableUseCase(chatId, 0)
        when (res) {
            is StatusTableUseCase.Result.Table -> {
                val markup = createPaginationMarkup(res.currentPage, res.hasPreviousPage, res.hasNextPage)
                sendMessage(chatId, res.tableText, markup)
            }
            is StatusTableUseCase.Result.Error -> sendMessage(chatId, res.msg)
        }
    }

    private fun createPaginationMarkup(currentPage: Int, hasPrev: Boolean, hasNext: Boolean): InlineKeyboardMarkup {
        val buttons = mutableListOf<InlineKeyboardButton>()
        if (hasPrev) {
            buttons.add(InlineKeyboardButton.CallbackData("⬅️", "${StatusTableUseCase.CALLBACK_PREFIX}${currentPage - 1}"))
        }
        buttons.add(InlineKeyboardButton.CallbackData("${currentPage + 1}", "__ignore"))
        if (hasNext) {
            buttons.add(InlineKeyboardButton.CallbackData("➡️", "${StatusTableUseCase.CALLBACK_PREFIX}${currentPage + 1}"))
        }
        return InlineKeyboardMarkup.create(listOf(buttons))
    }

    // MARK: easter

    private fun easterDispatcher(): (Dispatcher.() -> Unit) = {
        text {
            val chatId = ChatId.fromId(message.chat.id)
            val res = easterUseCase(text)
            when (res) {
                is EasterUseCase.Result.Document -> bot.sendPhoto(chatId, TelegramFile.ByFile(res.file))
                is EasterUseCase.Result.Dice -> bot.sendDice(chatId, DiceEmoji.SlotMachine)
                is EasterUseCase.Result.Unknown -> {}
            }
        }
    }

    // MARK: common

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

    private fun shareContactMarkup(label: String) = KeyboardReplyMarkup(
        keyboard = listOf(
            listOf(KeyboardButton(label, requestContact = true)),
        ),
        resizeKeyboard = true
    )

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
