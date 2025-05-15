package wedbot

enum class Command(val cmd: String, val description: String) {
    START(
        "start",
        "Пере/Запустить бота"
    ),
    SAVE_CALENDAR(
        "ics",
        "Добавить мероприятие в свой календарь"
    ),
    CHANGE_STATUS(
        "status",
        "Изменить статус посещения мероприятия"
    ),
    INVITE_GUEST(
        "invite",
        "Пригласить своего +1"
    )
}

object SystemProperties {
    val botToken: String = System.getProperty("bot.token")
    val botHost: String = System.getProperty("bot.host")
    val botPort: Int = System.getProperty("bot.port").toIntOrNull() ?: 80
    val keystorePassword: String = System.getProperty("keystore.pswd")
    val useWebhook: Boolean = System.getProperty("bot.webhook").toBoolean()
    val loggerOn: Boolean = System.getProperty("debug").toBoolean()
    val dbNeedInit: Boolean = System.getProperty("db.init").toBoolean()
}
