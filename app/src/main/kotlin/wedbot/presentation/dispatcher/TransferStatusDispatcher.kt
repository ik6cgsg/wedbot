package wedbot.presentation.dispatcher

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.entities.ChatId
import wedbot.domain.repository.TextRepository
import wedbot.domain.usecase.HandleTransferStatusUseCase
import wedbot.presentation.util.ReplyMarkupHelper
import wedbot.presentation.util.editSafeMessage
import wedbot.presentation.util.sendSafeMessage
import java.util.logging.Logger

class TransferStatusDispatcher(
    private val textRepository: TextRepository,
    private val handleTransferStatusUseCase: HandleTransferStatusUseCase,
) {
    private val logger = Logger.getLogger(this::class.java.name)

    fun setup(dispatcher: Dispatcher) {
        with(dispatcher) {
            handleTransferStatusUseCase.callbacks.forEach { data ->
                callbackQuery(data) {
                    val chatId = callbackQuery.from.id
                    logger.info(">>> START transferStatusDispatcher($data) for $chatId")
                    val input = HandleTransferStatusUseCase.Input.Query(data)
                    val res = handleTransferStatusUseCase(chatId, input)
                    when (res) {
                        is HandleTransferStatusUseCase.Result.DeleteWithAlert -> {
                            bot.answerCallbackQuery(
                                callbackQuery.id,
                                text = res.alert,
                                showAlert = true
                            )
                            callbackQuery.message?.messageId?.let {
                                bot.deleteMessage(ChatId.fromId(chatId), it)
                            }
                        }
                        is HandleTransferStatusUseCase.Result.Edit -> {
                            bot.editSafeMessage(
                                chatId = chatId,
                                messageId = callbackQuery.message?.messageId,
                                text = res.text
                            )
                            bot.sendSafeMessage(chatId, textRepository.menuSurveysDoubleClick())
                        }
                        is HandleTransferStatusUseCase.Result.Error -> {
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
                    logger.info("<<< END transferStatusDispatcher($data)")
                    logger.fine("with $res")
                }
            }
        }
    }

    fun pingTransferStatus(bot: Bot, chatId: Long) {
        logger.info(">>> START pingTransferStatus for $chatId")
        val input = HandleTransferStatusUseCase.Input.Ping
        val res = handleTransferStatusUseCase(chatId, input)
        when (res) {
            is HandleTransferStatusUseCase.Result.Send -> {
                bot.sendSafeMessage(chatId, res.text,
                    ReplyMarkupHelper.createInlineMarkup(res.buttonList)
                )
            }
            else -> {
                bot.sendSafeMessage(chatId, textRepository.internalError())
            }
        }
        logger.info("<<< END pingTransferStatus")
        logger.fine("with $res")
    }
}
