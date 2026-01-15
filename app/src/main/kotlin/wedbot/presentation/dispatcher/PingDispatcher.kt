package wedbot.presentation.dispatcher

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.ReplyKeyboardRemove
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import kotlinx.coroutines.delay
import wedbot.domain.repository.TextRepository
import wedbot.domain.usecase.PingGuestsUseCase
import wedbot.presentation.util.editSafeMessage
import wedbot.presentation.util.sendSafeMessage
import java.util.Collections
import java.util.logging.Logger

class PingDispatcher(
    private val textRepository: TextRepository,
    private val pingGuestsUseCase: PingGuestsUseCase,
) {
    private val logger = Logger.getLogger(this::class.java.name)
    private val adminsInPingMode = Collections.synchronizedSet(mutableSetOf<Long>())

    fun setup(dispatcher: Dispatcher) {
        with(dispatcher) {
            callbackQuery(PingGuestsUseCase.COMMAND_CANCEL) {
                val chatId = callbackQuery.from.id
                logger.info(">>> START pingDispatcher(cancel callback) for $chatId")
                if (adminsInPingMode.contains(chatId)) {
                    adminsInPingMode.remove(chatId)
                    bot.answerCallbackQuery(callbackQuery.id, text = textRepository.adminPingCancel())
                    bot.editSafeMessage(
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
                    bot.sendSafeMessage(id, fullText)
                    delay(50)
                }
                adminsInPingMode.remove(chatId)
                bot.sendSafeMessage(chatId, textRepository.adminPingSucceed())
                logger.info("<<< END pingDispatcher(broadcast) to ${chats.size} users")
                update.consume()
            }
        }
    }

    fun isUserInPingMode(chatId: Long): Boolean {
        return adminsInPingMode.contains(chatId)
    }

    fun startPingGuestsFlow(bot: Bot, chatId: Long) {
        bot._startPingGuestsFlow(chatId)
    }

    private fun Bot._startPingGuestsFlow(chatId: Long) {
        logger.info(">>> START startPingGuestsFlow for $chatId")
        val res = pingGuestsUseCase.checkRights(chatId)
        when (res) {
            is PingGuestsUseCase.CheckResult.Allowed -> {
                adminsInPingMode.add(chatId)
                sendSafeMessage(chatId, res.startMessage, ReplyKeyboardRemove())
                val cancelMarkup = InlineKeyboardMarkup.create(
                    listOf(InlineKeyboardButton.CallbackData(
                        res.cancelButton,
                        PingGuestsUseCase.COMMAND_CANCEL)
                    )
                )
                sendSafeMessage(chatId, res.prompt, cancelMarkup)
            }
            is PingGuestsUseCase.CheckResult.Error -> sendSafeMessage(chatId, res.msg)
        }
        logger.info("<<< END startPingGuestsFlow")
        logger.fine("with $res")
    }
}