package wedbot.presentation.dispatcher

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ReplyKeyboardRemove
import kotlinx.coroutines.delay
import wedbot.domain.repository.TextRepository
import wedbot.domain.usecase.PingGuestsUseCase
import wedbot.presentation.util.ReplyMarkupHelper
import wedbot.presentation.util.editSafeMessage
import wedbot.presentation.util.sendSafeMessage
import java.util.logging.Logger

class PingDispatcher(
    private val textRepository: TextRepository,
    private val pingGuestsUseCase: PingGuestsUseCase,
) {
    private val logger = Logger.getLogger(this::class.java.name)

    fun setup(dispatcher: Dispatcher) {
        with(dispatcher) {
            PingGuestsUseCase.QueryCommand.entries.forEach { queryCommand ->
                callbackQuery(queryCommand.id) {
                    val chatId = callbackQuery.from.id
                    logger.info(">>> START pingDispatcher(callbackQuery='${queryCommand.id}') for $chatId")
                    val caseInput = PingGuestsUseCase.Input.Query(queryCommand)
                    when (val res = pingGuestsUseCase.invoke(chatId, caseInput)) {
                        is PingGuestsUseCase.Result.UpdateQuery -> bot.editSafeMessage(
                            chatId = chatId,
                            messageId = callbackQuery.message?.messageId,
                            text = res.text,
                            ReplyMarkupHelper.createInlineMarkup(res.buttonList)
                        )
                        is PingGuestsUseCase.Result.Error -> bot.editSafeMessage(
                            chatId = chatId,
                            messageId = callbackQuery.message?.messageId,
                            text = res.msg,
                            ReplyKeyboardRemove()
                        )
                        else -> bot.editSafeMessage(
                            chatId = chatId,
                            messageId = callbackQuery.message?.messageId,
                            text = textRepository.internalError(),
                            ReplyKeyboardRemove()
                        )
                    }
                    bot.answerCallbackQuery(callbackQuery.id)
                    logger.info("<<< END pingDispatcher(callbackQuery='${queryCommand.id}')")
                }
            }
            text {
                val msg = message.text
                if (msg.isNullOrEmpty() || msg.startsWith("/")) return@text
                val chatId = message.chat.id
                if (!isUserInPingMode(chatId)) return@text
                logger.info(">>> START pingDispatcher(broadcast) for $chatId")
                val caseInput = PingGuestsUseCase.Input.SendText
                val res = pingGuestsUseCase.invoke(chatId, caseInput)
                when (res) {
                    is PingGuestsUseCase.Result.PingRecipients -> {
                        res.chatIdList.forEach { rcvId ->
                            val fullText = textRepository.adminMessageHeader() + "\n\n" + msg
                            bot.sendSafeMessage(rcvId, fullText)
                            delay(50)
                        }
                        bot.sendSafeMessage(chatId, textRepository.adminPingSucceed())
                        logger.info("--- pingDispatcher() pinged ${res.chatIdList.size} users")
                    }
                    is PingGuestsUseCase.Result.Error -> bot.sendSafeMessage(chatId, res.msg)
                    else -> bot.sendSafeMessage(chatId, textRepository.internalError())
                }
                logger.info("<<< END pingDispatcher(broadcast)")
                logger.fine("with $res")
                update.consume()
            }
        }
    }

    fun isUserInPingMode(chatId: Long): Boolean {
        return pingGuestsUseCase.isUserInPingMode(chatId)
    }

    fun startPingGuestsFlow(bot: Bot, chatId: Long) {
        logger.info(">>> START startPingGuestsFlow for $chatId")
        val caseInput = PingGuestsUseCase.Input.Start
        val res = pingGuestsUseCase.invoke(chatId, caseInput)
        when (res) {
            is PingGuestsUseCase.Result.Start -> {
                bot.sendSafeMessage(chatId, res.startMessage, ReplyKeyboardRemove())
                val inlineMarkup = ReplyMarkupHelper.createInlineMarkup(res.buttonList)
                bot.sendSafeMessage(chatId, res.prompt, inlineMarkup)
            }
            is PingGuestsUseCase.Result.Error -> bot.sendSafeMessage(chatId, res.msg)
            else -> bot.sendSafeMessage(chatId, textRepository.internalError())
        }
        logger.info("<<< END startPingGuestsFlow")
        logger.fine("with $res")
    }
}