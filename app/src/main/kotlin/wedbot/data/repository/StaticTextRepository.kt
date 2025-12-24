package wedbot.data.repository

import wedbot.data.resources.BotMessages
import wedbot.domain.repository.TextRepository

class StaticTextRepository : TextRepository {
    override fun generateGreeting(name: String?): String {
        return BotMessages.GREETING.format(name ?: "гость")
    }

    override fun shareContactError(): String = BotMessages.ASK_PHONE

    override fun shareContactLabel(): String = BotMessages.ASK_PHONE_LABEL

    override fun notFound(): String = BotMessages.USER_NOT_FOUND

    override fun alreadyRegistered(): String = BotMessages.USER_ALREADY_REGISTERED
}
