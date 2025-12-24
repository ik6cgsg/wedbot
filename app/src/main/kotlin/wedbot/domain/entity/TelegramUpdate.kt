package wedbot.domain.entity

data class TelegramUpdate(
    val chatId: Long,
    val text: String,
    val username: String?
)