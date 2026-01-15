package wedbot.presentation.dispatcher

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import wedbot.domain.entity.Status
import wedbot.domain.repository.TextRepository
import wedbot.domain.usecase.HandleEventStatusUseCase
import wedbot.presentation.util.ReplyMarkupHelper
import wedbot.presentation.util.editSafeMessage
import wedbot.presentation.util.sendSafeMessage
import java.util.logging.Logger

class EventStatusDispatcher(
    private val textRepository: TextRepository,
    private val handleEventStatusUseCase: HandleEventStatusUseCase,
) {
    private val logger = Logger.getLogger(this::class.java.name)

    fun setup(dispatcher: Dispatcher) {
        with(dispatcher) {
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
                            bot.editSafeMessage(
                                chatId = chatId,
                                messageId = callbackQuery.message?.messageId,
                                text = res.newText
                            )
                            if (res.newStatus != Status.SLEEVE) {
                                bot.sendSafeMessage(chatId, textRepository.menuUpdated())
                            }
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
    }

    fun pingEventStatus(bot: Bot, chatId: Long, name: String?) {
        bot.pingEventStatusDaily(chatId, name)
    }

    fun pingEventStatusFirst(bot: Bot, chatId: Long) {
        bot._pingEventStatusFirst(chatId)
    }

    private fun Bot.pingEventStatusDaily(chatId: Long, name: String?) {
        sendSafeMessage(chatId,
            textRepository.eventStatusPingDaily(name),
            ReplyMarkupHelper.createInlineMarkup(handleEventStatusUseCase.queries.map {
                InlineKeyboardButton.CallbackData(it.text, it.callback)
            })
        )
    }

    private fun Bot._pingEventStatusFirst(chatId: Long) {
        sendSafeMessage(
            chatId,
            textRepository.eventStatusPingFirst(),
            ReplyMarkupHelper.createInlineMarkup(handleEventStatusUseCase.queries.map {
                InlineKeyboardButton.CallbackData(it.text, it.callback)
            })
        )
    }
}