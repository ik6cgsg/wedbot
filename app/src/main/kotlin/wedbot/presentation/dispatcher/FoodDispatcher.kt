package wedbot.presentation.dispatcher

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.handlers.CallbackQueryHandlerEnvironment
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId
import wedbot.domain.repository.TextRepository
import wedbot.domain.usecase.HandleFoodUseCase
import wedbot.presentation.util.ReplyMarkupHelper
import wedbot.presentation.util.editSafeMessage
import wedbot.presentation.util.sendSafeMessage
import java.util.logging.Logger

class FoodDispatcher(
    private val textRepository: TextRepository,
    private val handleFoodUseCase: HandleFoodUseCase
) {
    private val logger = Logger.getLogger(this::class.java.name)

    fun setup(dispatcher: Dispatcher) {
        with(dispatcher) {
            handleFoodUseCase.menuCallbacks.forEach { data ->
                callbackQuery(data) {
                    val chatId = callbackQuery.from.id
                    logger.info(">>> START foodDispatcher($data) for $chatId")
                    val input = HandleFoodUseCase.Input.MenuQuery(data)
                    val res = handleFoodUseCase(chatId, input)
                    handleFoodResultFromQuery(res, this)
                    logger.info("<<< END foodDispatcher($data)")
                    logger.fine("with $res")
                }
            }
            handleFoodUseCase.drinkCallbacks.forEach { data ->
                callbackQuery(data) {
                    val chatId = callbackQuery.from.id
                    logger.info(">>> START drinkDispatcher($data) for $chatId")
                    val input = HandleFoodUseCase.Input.DrinkQuery(data)
                    val res = handleFoodUseCase(chatId, input)
                    handleFoodResultFromQuery(res, this)
                    logger.info("<<< END drinkDispatcher($data)")
                    logger.fine("with $res")
                }
            }
            callbackQuery(handleFoodUseCase.callbackSkipAdditional) {
                val chatId = callbackQuery.from.id
                logger.info(">>> START skipping additional food info for $chatId")
                val input = HandleFoodUseCase.Input.AdditionalInfo("")
                val res = handleFoodUseCase(chatId, input)
                handleFoodResultFromQuery(res, this)
                logger.info("<<< END additional food info skipped")
                logger.fine("with $res")
            }
            text {
                val chatId = message.chat.id
                if (!isUserInEnteringMode(chatId)) return@text
                logger.info(">>> START additionalDispatcher for $chatId")
                val text = message.text ?: ""
                val res = handleFoodUseCase(chatId, HandleFoodUseCase.Input.AdditionalInfo(text))
                when (res) {
                    is HandleFoodUseCase.Result.Finished -> {
                        bot.sendSafeMessage(chatId, res.text)
                        bot.sendSafeMessage(chatId, textRepository.menuSurveysDoubleClick())
                    }
                    is HandleFoodUseCase.Result.Error -> bot.sendSafeMessage(chatId, res.msg)
                    else -> bot.sendSafeMessage(chatId, textRepository.internalError())
                }
                logger.info("<<< END additionalDispatcher")
                logger.fine("with $res")
                update.consume()
            }
        }
    }

    fun startFoodDrinkSurvey(bot: Bot, chatId: Long) {
        logger.info(">>> START startFoodDrinkSurvey for $chatId")
        val input = HandleFoodUseCase.Input.Ping
        val res = handleFoodUseCase(chatId, input)
        when (res) {
            is HandleFoodUseCase.Result.Send -> bot.sendSafeMessage(chatId, res.text,
                ReplyMarkupHelper.createInlineMarkup(res.buttonList)
            )
            else -> bot.sendSafeMessage(chatId, textRepository.internalError())
        }
        logger.info("<<< END startFoodDrinkSurvey")
        logger.fine("with $res")
    }

    fun isUserInEnteringMode(chatId: Long) = handleFoodUseCase.isUserInEnteringMode(chatId)

    private fun handleFoodResultFromQuery(res: HandleFoodUseCase.Result, env: CallbackQueryHandlerEnvironment) {
        with (env) {
            val chatId = callbackQuery.from.id
            when (res) {
                is HandleFoodUseCase.Result.Send -> bot.sendSafeMessage(chatId, res.text,
                    ReplyMarkupHelper.createInlineMarkup(res.buttonList)
                )
                is HandleFoodUseCase.Result.Edit -> bot.editSafeMessage(
                    chatId = chatId,
                    messageId = callbackQuery.message?.messageId,
                    text = res.text,
                    ReplyMarkupHelper.createInlineMarkup(res.buttonList)
                )
                is HandleFoodUseCase.Result.Finished -> {
                    bot.editSafeMessage(
                        chatId = chatId,
                        messageId = callbackQuery.message?.messageId,
                        text = res.text
                    )
                    bot.sendSafeMessage(chatId, textRepository.menuSurveysDoubleClick())
                }
                is HandleFoodUseCase.Result.Error -> {
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
