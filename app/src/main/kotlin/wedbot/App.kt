package wedbot

fun main() {
    val dbUtils = DBUtils()
    val wedbot = WedBot(dbUtils)
    wedbot.start()
    if (SystemProperties.useWebhook) {
        val server = Server(wedbot)
        server.start()
    }
}
