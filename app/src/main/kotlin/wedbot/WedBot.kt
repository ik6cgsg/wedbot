package wedbot

import com.github.kotlintelegrambot.*
import com.github.kotlintelegrambot.dispatcher.*
import com.github.kotlintelegrambot.entities.*
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.entities.dice.DiceEmoji
import com.github.kotlintelegrambot.logging.LogLevel
import com.github.kotlintelegrambot.extensions.filters.Filter
import kotlin.random.Random
import wedbot.ScenarioAuth

class WedBot(
    private val dbUtils: DBUtils,
) {
    private val bot: Bot
    private val scenarioAuth: ScenarioAuth
    private val scenarioInvite: ScenarioInvite
    private val scenarioStatus: ScenarioStatus
    private val scenarioCalendar: ScenarioCalendar

    init {
        val debug = System.getProperty("debug", "false").toBoolean()
        bot = bot {
            if (debug) logLevel = LogLevel.All()
            token = System.getProperty("bot.token")
            dispatch(dispatcher())
        }
        scenarioAuth = ScenarioAuth(bot, dbUtils)
        scenarioInvite = ScenarioInvite(bot, dbUtils)
        scenarioStatus = ScenarioStatus(bot, dbUtils)
        scenarioCalendar = ScenarioCalendar(bot, dbUtils)
    }

    fun dispatcher(): (Dispatcher.() -> Unit) = {
        command(CommandName.start) {
            val args = message.text?.split(" ")
            if (args?.size == 2) {
                scenarioInvite.handleInvitedUserStart(message.chat.id, args[1])
            } else {
                scenarioAuth.handleCommand(message)
            }
        }
        command(CommandName.invite) {
            scenarioInvite.handleCommand(message)
        }
        command(CommandName.changeStatus) {
            scenarioStatus.handleCommand(message)
        }
        command(CommandName.saveCalendar) {
            scenarioCalendar.handleCommand(message)
        }
        contact {
            scenarioAuth.handleContact(message.chat.id, contact.phoneNumber, message.from?.username)
        }
        callbackQuery {
            if (callbackQuery.data.startsWith(CommandName.changeStatus)) {
                scenarioStatus.handleQuery(callbackQuery)
            } else if (callbackQuery.data.startsWith(CommandName.invite)) {
                scenarioInvite.handleQuery(callbackQuery)
            }
        }
        message(Filter.Sticker) {
            bot.sendMessage(ChatId.fromId(message.chat.id), UserMessage.sticker)
        }
        // TODO: easter egg + admin scenarios
        text("dep") {
            val cid = ChatId.fromId(message.chat.id)
            val diceList = listOf(DiceEmoji.Football, DiceEmoji.SlotMachine, DiceEmoji.Bowling, DiceEmoji.Basketball, DiceEmoji.Dartboard, DiceEmoji.Dice)
            val randomDice = diceList[Random.nextInt(diceList.size)]
            bot.sendDice(cid, randomDice)
        }
        telegramError {
            println(error.getErrorMessage())
        }
    }

    fun start() {
        // TODO: webhooking?
        bot.startPolling()
    }
}
