package wedbot.presentation

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.contact
import com.github.kotlintelegrambot.dispatcher.telegramError
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatAction
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.ReplyKeyboardRemove
import com.github.kotlintelegrambot.entities.ReplyMarkup
import com.github.kotlintelegrambot.entities.TelegramFile
import com.github.kotlintelegrambot.entities.dice.DiceEmoji
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import com.github.kotlintelegrambot.logging.LogLevel
import com.github.kotlintelegrambot.webhook
import kotlinx.coroutines.delay
import wedbot.SystemProperties
import wedbot.domain.entity.Status
import wedbot.domain.entity.UserStatus
import wedbot.domain.repository.TextRepository
import wedbot.domain.usecase.CalendarUseCase
import wedbot.domain.usecase.EasterUseCase
import wedbot.domain.usecase.HandleEventStatusUseCase
import wedbot.domain.usecase.InfoUseCase
import wedbot.domain.usecase.LocationUseCase
import wedbot.domain.usecase.MenuUseCase
import wedbot.domain.usecase.PingGuestsUseCase
import wedbot.domain.usecase.StartUseCase
import wedbot.domain.usecase.StatusTableUseCase
import wedbot.domain.usecase.VerifyPhoneUseCase
import wedbot.presentation.server.CertificateUtils
import java.util.Collections
import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger


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
    private val logger = Logger.getLogger(this::class.java.name)

    init {
        bot = bot {
            if (SystemProperties.loggerOn) {
                logger.level = Level.ALL
                val handler = ConsoleHandler()
                handler.level = Level.ALL
                logger.addHandler(handler)
                logLevel = LogLevel.All()
            } else {
                logger.level = Level.INFO
                logLevel = LogLevel.Error
            }
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
            logger.severe("Telegram Error: ${error.getErrorMessage()}")
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
            logger.info(">>> START authDispatcher(start) for $chatId")
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
            logger.info("<<< END authDispatcher(start)")
            logger.fine("with $res")
        }
        contact {
            val chatId = message.chat.id
            logger.info(">>> START authDispatcher(contact) for $chatId")
            val res = verifyPhoneUseCase(chatId, contact.phoneNumber, message.from?.username)
            when (res) {
                is VerifyPhoneUseCase.Result.UserFound -> {
                    userVerifiedAfterStart(chatId, res.greeting, res.status)
                }
                is VerifyPhoneUseCase.Result.Error -> sendMessage(chatId, res.msg)
            }
            logger.info("<<< END authDispatcher(contact)")
            logger.fine("with $res")
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
                logger.info(">>> START eventStatusDispatcher(${query.callback}) for $chatId")
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
                        editMessage(
                            chatId = chatId,
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
                logger.info("<<< END eventStatusDispatcher(${query.callback})")
                logger.fine("with $res")
            }
        }
    }

    // MARK: menu

    fun menuDispatcher(): (Dispatcher.() -> Unit) = {
        command(MenuUseCase.COMMAND_NAME) {
            val chatId = message.chat.id
            logger.info(">>> START menuDispatcher(command) for $chatId")
            val res = menuUseCase(message.chat.id)
            when (res) {
                is MenuUseCase.Result.Markup -> sendMessage(
                    chatId, res.message, createReplyMarkup(res.buttons)
                )
                is MenuUseCase.Result.Error -> sendMessage(chatId, res.msg)
            }
            logger.info("<<< END menuDispatcher(command)")
            logger.fine("with $res")
        }
        menuUseCase.buttonToLabel.forEach { (button, label) ->
            text(label) {
                val chatId = message.chat.id
                logger.info(">>> START menuDispatcher(${label}) for $chatId")
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
                logger.info("<<< END menuDispatcher(${label})")
            }
        }
    }

    private fun showInfo(chatId: Long) {
        logger.info(">>> START showInfo for $chatId")
        val res = infoUseCase(chatId)
        when (res) {
            is InfoUseCase.Result.Info -> sendMessage(chatId, res.text)
            is InfoUseCase.Result.Error -> sendMessage(chatId, res.msg)
        }
        logger.info("<<< END showInfo")
        logger.fine("with $res")
    }

    // MARK: ping guests

    private fun startPingGuestsFlow(chatId: Long) {
        logger.info(">>> START startPingGuestsFlow for $chatId")
        val res = pingGuestsUseCase.checkRights(chatId)
        when (res) {
            is PingGuestsUseCase.CheckResult.Allowed -> {
                adminsInPingMode.add(chatId)
                sendMessage(chatId, res.startMessage, ReplyKeyboardRemove())
                val cancelMarkup = InlineKeyboardMarkup.create(
                    listOf(InlineKeyboardButton.CallbackData(
                        res.cancelButton,
                        PingGuestsUseCase.COMMAND_CANCEL)
                    )
                )
                sendMessage(chatId, res.prompt, cancelMarkup)
            }
            is PingGuestsUseCase.CheckResult.Error -> sendMessage(chatId, res.msg)
        }
        logger.info("<<< END startPingGuestsFlow")
        logger.fine("with $res")
    }

    private fun pingDispatcher(): (Dispatcher.() -> Unit) = {
        callbackQuery(PingGuestsUseCase.COMMAND_CANCEL) {
            val chatId = callbackQuery.from.id
            logger.info(">>> START pingDispatcher(cancel callback) for $chatId")
            if (adminsInPingMode.contains(chatId)) {
                adminsInPingMode.remove(chatId)
                bot.answerCallbackQuery(callbackQuery.id, text = textRepository.adminPingCancel())
                editMessage(
                    chatId = chatId,
                    messageId = callbackQuery.message?.messageId,
                    text = textRepository.adminPingCancel()
                )
            } else {
                bot.answerCallbackQuery(callbackQuery.id, textRepository.internalError())
            }
            logger.info("<<< END pingDispatcher(cancel callback)")
        }
        text {
            val msg = message.text
            if (msg.isNullOrEmpty() || msg.startsWith("/")) return@text
            val chatId = message.chat.id
            if (!adminsInPingMode.contains(chatId)) return@text
            logger.info(">>> START pingDispatcher(broadcast) for $chatId")
            val chats = pingGuestsUseCase.getAllChats(chatId)
            chats.forEach { id ->
                val fullText = textRepository.adminMessageHeader() + "\n\n" + msg
                sendMessage(id, fullText)
                delay(50)
            }
            adminsInPingMode.remove(chatId)
            sendMessage(chatId, textRepository.adminPingSucceed())
            logger.info("<<< END pingDispatcher(broadcast) to ${chats.size} users")
            update.consume()
        }
    }

    // MARK: geo data info

    private fun showLocation(chatId: Long) {
        logger.info(">>> START showLocation for $chatId")
        val res = locationUseCase(chatId)
        when (res) {
            is LocationUseCase.Result.Location -> bot.sendLocation(
                ChatId.fromId(chatId),
                res.latitude, res.longitude
            )
            is LocationUseCase.Result.Error -> sendMessage(chatId, res.msg)
        }
        logger.info("<<< END showLocation")
        logger.fine("with $res")
    }

    private fun showCalendar(chatId: Long) {
        logger.info(">>> START showCalendar for $chatId")
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
        logger.info("<<< END showCalendar")
        logger.fine("with $res")
    }

    // MARK: status table

    private fun statusTableDispatcher(): (Dispatcher.() -> Unit) = {
        callbackQuery {
            if (callbackQuery.data.startsWith(StatusTableUseCase.CALLBACK_PREFIX)) {
                val page = callbackQuery.data.removePrefix(StatusTableUseCase.CALLBACK_PREFIX).toIntOrNull() ?: 0
                val chatId = callbackQuery.from.id
                logger.info(">>> START statusTableDispatcher(page=$page) for $chatId")
                val res = statusTableUseCase(chatId, page)
                when (res) {
                    is StatusTableUseCase.Result.Table -> {
                        val markup = createPaginationMarkup(res.currentPage, res.hasPreviousPage, res.hasNextPage)
                        editMessage(
                            chatId = chatId,
                            messageId = callbackQuery.message?.messageId,
                            text = res.tableText,
                            markup = markup
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
                logger.info("<<< END statusTableDispatcher(page=$page)")
                logger.fine("with $res")
                update.consume()
            }
        }
    }

    private fun showStatusTable(chatId: Long) {
        logger.info(">>> START showStatusTable for $chatId")
        val res = statusTableUseCase(chatId, 0)
        when (res) {
            is StatusTableUseCase.Result.Table -> {
                val markup = createPaginationMarkup(res.currentPage, res.hasPreviousPage, res.hasNextPage)
                sendMessage(chatId, res.tableText, markup)
            }
            is StatusTableUseCase.Result.Error -> sendMessage(chatId, res.msg)
        }
        logger.info("<<< END showStatusTable")
        logger.fine("with $res")
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
            if (res == EasterUseCase.Result.Unknown) return@text
            logger.info(">>> START easterDispatcher found for ${message.chat.id}")
            when (res) {
                is EasterUseCase.Result.Document -> bot.sendPhoto(chatId, TelegramFile.ByFile(res.file))
                is EasterUseCase.Result.Dice -> bot.sendDice(chatId, DiceEmoji.SlotMachine)
                else -> {}
            }
            logger.info("<<< END easterDispatcher found for ${message.chat.id}")
            update.consume()
        }
    }

    // MARK: common

    private fun sendMessage(
        chatId: Long,
        text: String,
        markup: ReplyMarkup = ReplyKeyboardRemove()
    ) {
        try {
            val chatIdTg = ChatId.fromId(chatId)
            bot.sendChatAction(chatIdTg, ChatAction.TYPING)
            bot.sendMessage(
                chatId = chatIdTg,
                text = text,
                parseMode = ParseMode.MARKDOWN,
                replyMarkup = markup
            )
        } catch (e: Throwable) {
            logger.log(Level.WARNING, "Failed to send message to $chatId", e)
        }
    }

    private fun editMessage(
        chatId: Long,
        messageId: Long?,
        text: String,
        markup: ReplyMarkup? = null
    ) {
        try {
            bot.editMessageText(
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
