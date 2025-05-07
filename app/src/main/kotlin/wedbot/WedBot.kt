package wedbot

import com.github.kotlintelegrambot.*
import com.github.kotlintelegrambot.dispatcher.*
import com.github.kotlintelegrambot.entities.*
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.logging.LogLevel
import com.github.kotlintelegrambot.extensions.filters.Filter

class WedBot(
    private val dbUtils: DBUtils,
) {
    private val bot: Bot
    private val scenarioAuth: ScenarioAuth
    private val scenarioInvite: ScenarioInvite
    private val scenarioStatus: ScenarioStatus
    private val scenarioCalendar: ScenarioCalendar
    private val scenarioAdmin: ScenarioAdmin
    private val scenarioEaster: ScenarioEaster

    init {
        val debug = System.getProperty("debug", "false").toBoolean()
        bot = bot {
            if (debug) logLevel = LogLevel.All()
            token = SystemProperties.botToken
            webhook {
                url = "https://${SystemProperties.botHost}:8443/${SystemProperties.botToken}"
                certificate = TelegramFile.ByFile(CertificateUtils.certPathFile)
                maxConnections = 20
                allowedUpdates = listOf("message", "callback_query")
            }
            dispatch(dispatcher())
        }
        scenarioAuth = ScenarioAuth(bot, dbUtils)
        scenarioInvite = ScenarioInvite(bot, dbUtils)
        scenarioStatus = ScenarioStatus(bot, dbUtils)
        scenarioCalendar = ScenarioCalendar(bot, dbUtils)
        scenarioAdmin = ScenarioAdmin(bot, dbUtils)
        scenarioEaster = ScenarioEaster(bot, dbUtils)
    }

    fun dispatcher(): (Dispatcher.() -> Unit) = {
        command(Command.START.cmd) {
            val args = message.text?.split(" ")
            if (args?.size == 2) {
                scenarioInvite.handleInvitedUserStart(message)
            } else {
                scenarioAuth.handleCommand(message)
            }
        }
        command(Command.INVITE_GUEST.cmd) {
            scenarioInvite.handleCommand(message)
        }
        command(Command.CHANGE_STATUS.cmd) {
            scenarioStatus.handleCommand(message)
        }
        command(Command.SAVE_CALENDAR.cmd) {
            scenarioCalendar.handleCommand(message)
        }
        contact {
            scenarioAuth.handleContact(message.chat.id, contact.phoneNumber, message.from?.username)
        }
        callbackQuery {
            if (callbackQuery.data.startsWith(Command.CHANGE_STATUS.cmd)) {
                scenarioStatus.handleQuery(callbackQuery)
            } else if (callbackQuery.data.startsWith(Command.INVITE_GUEST.cmd)) {
                scenarioInvite.handleQuery(callbackQuery)
            }  else if (callbackQuery.data.startsWith(TextCommandAdmin.prefix)) {
                scenarioAdmin.handleQuery(callbackQuery)
            }
        }
        message(Filter.Sticker) {
            bot.sendMessage(ChatId.fromId(message.chat.id), UserMessageEaster.sticker)
        }
        text {
            if (text.startsWith(TextCommandAdmin.prefix)) {
                scenarioAdmin.handleText(text, message.chat.id)
            } else {
                scenarioEaster.handleText(text, message.chat.id)
            }
        }
        telegramError {
            println(error.getErrorMessage())
        }
    }

    fun start() {
        // TODO: webhooking?
        //bot.startPolling()
        bot.startWebhook()
    }

    suspend fun process(message: String) {
        bot.processUpdate(message)
    }
}
