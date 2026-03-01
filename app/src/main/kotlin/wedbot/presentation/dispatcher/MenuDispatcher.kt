package wedbot.presentation.dispatcher

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.TelegramFile
import wedbot.domain.repository.TextRepository
import wedbot.domain.usecase.CalendarUseCase
import wedbot.domain.usecase.DressCodeUseCase
import wedbot.domain.usecase.InfoUseCase
import wedbot.domain.usecase.LocationUseCase
import wedbot.domain.usecase.MenuUseCase
import wedbot.presentation.util.ReplyMarkupHelper
import wedbot.presentation.util.sendSafeMessage
import java.util.logging.Logger

interface MenuEventInterface {
    fun needToHandle(bot: Bot, chatId: Long): Boolean
    fun pingEventStatusFirst(bot: Bot, chatId: Long)
    fun showStatusTable(bot: Bot, chatId: Long)
    fun startPingGuestsFlow(bot: Bot, chatId: Long)
    fun pingVillaStatus(bot: Bot, chatId: Long)
    fun pingTransferStatus(bot: Bot, chatId: Long)
    fun startFoodDrinkSurvey(bot: Bot, chatId: Long)
}

class MenuDispatcher(
    private val textRepository: TextRepository,
    private val menuUseCase: MenuUseCase,
    private val infoUseCase: InfoUseCase,
    private val dressCodeUseCase: DressCodeUseCase,
    private val calendarUseCase: CalendarUseCase,
    private val locationUseCase: LocationUseCase,
    private val menuEventHandler: MenuEventInterface
) {
    private val logger = Logger.getLogger(this::class.java.name)

    fun setup(dispatcher: Dispatcher) {
        with(dispatcher) {
            command(MenuUseCase.COMMAND_NAME) {
                val chatId = message.chat.id
                logger.info(">>> START menuDispatcher(command) for $chatId")
                val res = menuUseCase(message.chat.id)
                when (res) {
                    is MenuUseCase.Result.Markup -> bot.sendSafeMessage(
                        chatId, res.message, ReplyMarkupHelper.createReplyMarkup(res.buttons)
                    )
                    is MenuUseCase.Result.Error -> bot.sendSafeMessage(chatId, res.msg)
                }
                logger.info("<<< END menuDispatcher(command)")
                logger.fine("with $res")
            }
            menuUseCase.buttonToLabel.forEach { (button, label) ->
                text(label) {
                    val chatId = message.chat.id
                    logger.info(">>> START menuDispatcher(${label}) for $chatId")
                    if (!menuEventHandler.needToHandle(bot, chatId)) return@text
                    when (button) {
                        MenuUseCase.Button.INFO -> bot.showInfo(chatId)
                        MenuUseCase.Button.DRESS_CODE -> bot.showDressCode(chatId)
                        MenuUseCase.Button.ICS -> bot.showCalendar(chatId)
                        MenuUseCase.Button.LOCATION -> bot.showLocation(chatId)
                        MenuUseCase.Button.EVENT_STATUS -> menuEventHandler.pingEventStatusFirst(bot, chatId)
                        MenuUseCase.Button.HELP -> bot.sendSafeMessage(chatId, textRepository.helpMessage())
                        MenuUseCase.Button.STATUS_TABLE -> menuEventHandler.showStatusTable(bot, chatId)
                        MenuUseCase.Button.PING_GUESTS -> menuEventHandler.startPingGuestsFlow(bot, chatId)
                        MenuUseCase.Button.VILLA -> menuEventHandler.pingVillaStatus(bot, chatId)
                        MenuUseCase.Button.TRANSFER -> menuEventHandler.pingTransferStatus(bot, chatId)
                        MenuUseCase.Button.FOOD -> menuEventHandler.startFoodDrinkSurvey(bot, chatId)
                    }
                    logger.info("<<< END menuDispatcher(${label})")
                }
            }
        }
    }

    fun pingSurveys(bot: Bot, chatId: Long) {
        bot.sendSafeMessage(chatId, textRepository.menuHasSurveys())
    }

    private fun Bot.showInfo(chatId: Long) {
        logger.info(">>> START showInfo for $chatId")
        val res = infoUseCase(chatId)
        when (res) {
            is InfoUseCase.Result.Info -> sendSafeMessage(chatId, res.text)
            is InfoUseCase.Result.Error -> sendSafeMessage(chatId, res.msg)
        }
        logger.info("<<< END showInfo")
        logger.fine("with $res")
    }

    private fun Bot.showDressCode(chatId: Long) {
        logger.info(">>> START showDressCode for $chatId")
        val res = dressCodeUseCase(chatId)
        when (res) {
            is DressCodeUseCase.Result.DocumentInfo -> sendPhoto(
                chatId = ChatId.fromId(chatId),
                photo = TelegramFile.ByFile(res.path)
            )
            is DressCodeUseCase.Result.Error -> sendSafeMessage(chatId, res.msg)
        }
        logger.info("<<< END showDressCode")
        logger.fine("with $res")
    }

    private fun Bot.showLocation(chatId: Long) {
        logger.info(">>> START showLocation for $chatId")
        val res = locationUseCase(chatId)
        when (res) {
            is LocationUseCase.Result.Location -> sendLocation(
                ChatId.fromId(chatId),
                res.latitude, res.longitude
            )
            is LocationUseCase.Result.Error -> sendSafeMessage(chatId, res.msg)
        }
        logger.info("<<< END showLocation")
        logger.fine("with $res")
    }

    private fun Bot.showCalendar(chatId: Long) {
        logger.info(">>> START showCalendar for $chatId")
        val res = calendarUseCase(chatId)
        when (res) {
            is CalendarUseCase.Result.DocumentInfo -> sendDocument(
                chatId = ChatId.fromId(chatId),
                document = TelegramFile.ByFile(res.path),
                caption = res.caption
            )
            is CalendarUseCase.Result.Error -> sendSafeMessage(chatId, res.msg)
        }
        logger.info("<<< END showCalendar")
        logger.fine("with $res")
    }
}