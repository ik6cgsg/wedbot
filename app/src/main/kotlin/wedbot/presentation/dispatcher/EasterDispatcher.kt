package wedbot.presentation.dispatcher

import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.TelegramFile
import com.github.kotlintelegrambot.entities.dice.DiceEmoji
import wedbot.domain.usecase.EasterUseCase
import wedbot.presentation.util.sendSafeMessage
import java.util.logging.Logger

class EasterDispatcher(
    private val easterUseCase: EasterUseCase
) {
    private val logger = Logger.getLogger(this::class.java.name)

    fun setup(dispatcher: Dispatcher) {
        with(dispatcher) {
            text {
                val chatId = ChatId.fromId(message.chat.id)
                val res = easterUseCase(text)
                if (res == EasterUseCase.Result.Unknown) return@text
                logger.info(">>> START easterDispatcher found for ${message.chat.id}")
                when (res) {
                    is EasterUseCase.Result.Document -> bot.sendPhoto(chatId, TelegramFile.ByFile(res.file))
                    is EasterUseCase.Result.Dice -> bot.sendDice(chatId, DiceEmoji.SlotMachine)
                    is EasterUseCase.Result.Text -> bot.sendSafeMessage(message.chat.id, res.msg)
                    else -> {}
                }
                logger.info("<<< END easterDispatcher found for ${message.chat.id}")
                update.consume()
            }
        }
    }
}