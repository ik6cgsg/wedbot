package wedbot.presentation.dispatcher

import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.text
import wedbot.domain.repository.TextRepository
import wedbot.presentation.util.sendSafeMessage
import java.util.logging.Logger

class DummyDispatcher(
    private val textRepository: TextRepository,
) {
    private val logger = Logger.getLogger(this::class.java.name)

    fun setup(dispatcher: Dispatcher) {
        with(dispatcher) {
            text {
                logger.info(">>> START dummyDispatcher for ${message.chat.id}")
                bot.sendSafeMessage(message.chat.id, textRepository.techWorks())
                logger.info("<<< END dummyDispatcher")
            }
        }
    }
}