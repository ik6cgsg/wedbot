package wedbot.presentation.dispatcher

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.contact
import com.github.kotlintelegrambot.entities.ChatAction
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.TelegramFile
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import kotlinx.coroutines.delay
import wedbot.BotConstants
import wedbot.domain.entity.Status
import wedbot.domain.entity.UserStatus
import wedbot.domain.repository.TextRepository
import wedbot.domain.usecase.HandleEventStatusUseCase
import wedbot.domain.usecase.StartUseCase
import wedbot.domain.usecase.VerifyPhoneUseCase
import wedbot.presentation.util.ReplyMarkupHelper
import wedbot.presentation.util.sendSafeMessage
import java.io.File
import java.util.logging.Logger

class AuthDispatcher(
    private val textRepository: TextRepository,
    private val startUseCase: StartUseCase,
    private val verifyPhoneUseCase: VerifyPhoneUseCase,
    private val pingEventStatusFirst: (Bot, Long) -> Unit
) {
    private val logger = Logger.getLogger(this::class.java.name)

    fun setup(dispatcher: Dispatcher) {
        with(dispatcher) {
            command(StartUseCase.COMMAND_NAME) {
                val chatId = message.chat.id
                logger.info(">>> START authDispatcher(start) for $chatId")
                val res = startUseCase(chatId, message.from?.username)
                when (res) {
                    is StartUseCase.Result.UserFound -> {
                        bot.userVerifiedAfterStart(chatId, res.greeting, res.status)
                    }
                    is StartUseCase.Result.NeedPhoneCheck -> bot.sendSafeMessage(
                        chatId, res.error,
                        ReplyMarkupHelper.shareContactMarkup(res.label)
                    )
                    is StartUseCase.Result.Error -> bot.sendSafeMessage(chatId, res.msg)
                }
                logger.info("<<< END authDispatcher(start)")
                logger.fine("with $res")
            }
            contact {
                val chatId = message.chat.id
                logger.info(">>> START authDispatcher(contact) for $chatId")
                val res = verifyPhoneUseCase(chatId, contact.phoneNumber, message.from?.username)
                when (res) {
                    is VerifyPhoneUseCase.Result.UserFound -> {
                        bot.userVerifiedAfterStart(chatId, res.greeting, res.status)
                    }
                    is VerifyPhoneUseCase.Result.Error -> bot.sendSafeMessage(chatId, res.msg)
                }
                logger.info("<<< END authDispatcher(contact)")
                logger.fine("with $res")
            }
        }
    }

    private suspend fun Bot.userVerifiedAfterStart(chatId: Long, greeting: String, status: UserStatus) {
        if (status.eventStatus == Status.SLEEVE) {
            sendSafeMessage(chatId, textRepository.internalError())
        } else {
            sendPhoto(ChatId.fromId(chatId), TelegramFile.ByFile(File(BotConstants.invitePhotoPath)))
            sendSafeMessage(chatId, greeting)
        }
        if (status.eventStatus == Status.THINKING) {
            sendChatAction(ChatId.fromId(chatId), ChatAction.TYPING)
            delay(1000)
            pingEventStatusFirst(this, chatId)
        }
    }
}
