package wedbot.presentation

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.telegramError
import com.github.kotlintelegrambot.entities.TelegramFile
import com.github.kotlintelegrambot.logging.LogLevel
import com.github.kotlintelegrambot.webhook
import wedbot.SystemProperties
import wedbot.presentation.dispatcher.AuthDispatcher
import wedbot.presentation.dispatcher.DummyDispatcher
import wedbot.presentation.dispatcher.EasterDispatcher
import wedbot.presentation.dispatcher.EventStatusDispatcher
import wedbot.presentation.dispatcher.MenuDispatcher
import wedbot.presentation.dispatcher.PingDispatcher
import wedbot.presentation.dispatcher.StatusTableDispatcher
import wedbot.presentation.dispatcher.TransferStatusDispatcher
import wedbot.presentation.dispatcher.VillaStatusDispatcher
import wedbot.presentation.server.CertificateUtils
import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger


class WedBot(
    private val authDispatcher: AuthDispatcher,
    private val eventStatusDispatcher: EventStatusDispatcher,
    private val menuDispatcher: MenuDispatcher,
    private val statusTableDispatcher: StatusTableDispatcher,
    private val pingDispatcher: PingDispatcher,
    private val easterDispatcher: EasterDispatcher,
    private val dummyDispatcher: DummyDispatcher,
    private val villaStatusDispatcher: VillaStatusDispatcher,
    private val transferStatusDispatcher: TransferStatusDispatcher
) {
    private val bot: Bot
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
                url = "https://${SystemProperties.botHost}:${SystemProperties.botPort}/${SystemProperties.botToken}"
                certificate = TelegramFile.ByFile(CertificateUtils.certPathFile)
                maxConnections = 20
                dropPendingUpdates = true
                allowedUpdates = listOf("message", "callback_query")
            }
            dispatch(dispatcher())
        }
    }

    private fun dispatcher(): (Dispatcher.() -> Unit) = {
        if (SystemProperties.dummy) {
            dummyDispatcher.setup(this)
        } else {
            pingDispatcher.setup(this)
            authDispatcher.setup(this)
            eventStatusDispatcher.setup(this)
            villaStatusDispatcher.setup(this)
            transferStatusDispatcher.setup(this)
            menuDispatcher.setup(this)
            statusTableDispatcher.setup(this)
            easterDispatcher.setup(this)
        }
        telegramError {
            logger.severe("Telegram Error: ${error.getErrorMessage()}")
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
        eventStatusDispatcher.pingEventStatus(bot, chatId, name)
    }

    fun pingSurveys(chatId: Long) {
        menuDispatcher.pingSurveys(bot, chatId)
    }
}
