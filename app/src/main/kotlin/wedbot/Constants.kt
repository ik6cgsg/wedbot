package wedbot

object SystemProperties {
    val botToken: String = System.getProperty("bot.token")
    val botHost: String = System.getProperty("bot.host")
    val botPort: Int = System.getProperty("bot.port").toIntOrNull() ?: 80
    val keystorePassword: String = System.getProperty("keystore.pswd")
    val useWebhook: Boolean = System.getProperty("bot.webhook").toBoolean()
    val webhookPath: String = System.getProperty("bot.webhook.path") ?: "telegram-webhook"
    val loggerOn: Boolean = System.getProperty("mode.debug").toBoolean()
    val dummy: Boolean = System.getProperty("mode.dummy").toBoolean()
    val dbNeedInit: Boolean = System.getProperty("db.init").toBoolean()
}

object BotConstants {
    const val weddingDeadline = "27.03.2026"
    const val eventStatusDeadline = "06.01.2026"
    const val villaStatusDeadline = "15.02.2026"
    const val transferStatusDeadline = "15.02.2026"
    const val surveysDeadline = "15.03.2026"
    const val calendarFilePath = "res/wed2.ics"
    const val invitePhotoPath = "res/wed2.png"
    const val locationCoordinates = "59.646974, 30.516003"
    const val dressCodeFilePath = "res/dress.png"
}
