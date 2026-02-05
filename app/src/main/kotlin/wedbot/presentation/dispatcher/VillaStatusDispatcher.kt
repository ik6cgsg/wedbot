package wedbot.presentation.dispatcher

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.entities.ChatId
import wedbot.domain.repository.TextRepository
import wedbot.domain.usecase.HandleVillaStatusUseCase
import wedbot.presentation.util.ReplyMarkupHelper
import wedbot.presentation.util.editSafeMessage
import wedbot.presentation.util.sendSafeMessage
import java.util.logging.Logger

class VillaStatusDispatcher(
    private val textRepository: TextRepository,
    private val handleVillaStatusUseCase: HandleVillaStatusUseCase,
) {
    private val logger = Logger.getLogger(this::class.java.name)

    fun setup(dispatcher: Dispatcher) {
        with(dispatcher) {
            HandleVillaStatusUseCase.QueryCommand.entries.forEach { queryCommand ->
                callbackQuery(queryCommand.id) {
                    val chatId = callbackQuery.from.id
                    logger.info(">>> START villaStatusDispatcher(${queryCommand.id}) for $chatId")
                    val input = HandleVillaStatusUseCase.Input.Query(queryCommand)
                    val res = handleVillaStatusUseCase(chatId, input)
                    when (res) {
                        is HandleVillaStatusUseCase.Result.DeleteWithAlert -> {
                            bot.answerCallbackQuery(
                                callbackQuery.id,
                                text = res.alert,
                                showAlert = true
                            )
                            callbackQuery.message?.messageId?.let {
                                bot.deleteMessage(ChatId.fromId(chatId), it)
                            }
                        }
                        is HandleVillaStatusUseCase.Result.Edit -> {
                            bot.editSafeMessage(
                                chatId = chatId,
                                messageId = callbackQuery.message?.messageId,
                                text = res.text
                            )
                            bot.sendSafeMessage(chatId, textRepository.menuSurveysDoubleClick())
                        }
                        is HandleVillaStatusUseCase.Result.Error -> {
                            bot.answerCallbackQuery(
                                callbackQuery.id,
                                text = res.msg,
                                showAlert = false
                            )
                            callbackQuery.message?.messageId?.let {
                                bot.deleteMessage(ChatId.fromId(chatId), it)
                            }
                        }
                        else -> {
                            bot.answerCallbackQuery(callbackQuery.id)
                            callbackQuery.message?.messageId?.let {
                                bot.deleteMessage(ChatId.fromId(chatId), it)
                            }
                        }
                    }
                    logger.info("<<< END villaStatusDispatcher(${queryCommand.id})")
                    logger.fine("with $res")
                }
            }
        }
    }

    fun pingVillaStatus(bot: Bot, chatId: Long) {
        logger.info(">>> START pingVillaStatus for $chatId")
        val input = HandleVillaStatusUseCase.Input.Ping
        val res = handleVillaStatusUseCase(chatId, input)
        when (res) {
            is HandleVillaStatusUseCase.Result.Send -> {
                bot.sendSafeMessage(chatId, res.text,
                    ReplyMarkupHelper.createInlineMarkup(res.buttonList)
                )
            }
            else -> {
                bot.sendSafeMessage(chatId, textRepository.internalError())
            }
        }
        logger.info("<<< END pingVillaStatus")
        logger.fine("with $res")
    }
}