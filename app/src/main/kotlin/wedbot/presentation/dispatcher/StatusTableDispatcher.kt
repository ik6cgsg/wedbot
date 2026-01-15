package wedbot.presentation.dispatcher

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import wedbot.domain.usecase.StatusTableUseCase
import wedbot.presentation.util.editSafeMessage
import wedbot.presentation.util.sendSafeMessage
import java.util.logging.Logger

class StatusTableDispatcher(
    private val statusTableUseCase: StatusTableUseCase,
) {
    private val logger = Logger.getLogger(this::class.java.name)

    fun setup(dispatcher: Dispatcher) {
        with(dispatcher) {
            callbackQuery {
                if (callbackQuery.data.startsWith(StatusTableUseCase.CALLBACK_PREFIX)) {
                    val page = callbackQuery.data.removePrefix(StatusTableUseCase.CALLBACK_PREFIX).toIntOrNull() ?: 0
                    val chatId = callbackQuery.from.id
                    logger.info(">>> START statusTableDispatcher(page=$page) for $chatId")
                    val res = statusTableUseCase(chatId, page)
                    when (res) {
                        is StatusTableUseCase.Result.Table -> {
                            val markup = createPaginationMarkup(res.currentPage, res.hasPreviousPage, res.hasNextPage)
                            bot.editSafeMessage(
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
    }

    fun showStatusTable(bot: Bot, chatId: Long) {
        logger.info(">>> START showStatusTable for $chatId")
        val res = statusTableUseCase(chatId, 0)
        when (res) {
            is StatusTableUseCase.Result.Table -> {
                val markup = createPaginationMarkup(res.currentPage, res.hasPreviousPage, res.hasNextPage)
                bot.sendSafeMessage(chatId, res.tableText, markup)
            }
            is StatusTableUseCase.Result.Error -> bot.sendSafeMessage(chatId, res.msg)
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
}