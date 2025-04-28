package wedbot

fun main() {
    val dbUtils = DBUtils()
    val wedbot = WedBot(dbUtils)
    wedbot.start()
}
