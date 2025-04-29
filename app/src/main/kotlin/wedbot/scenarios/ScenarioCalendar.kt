package wedbot

import com.github.kotlintelegrambot.*
import com.github.kotlintelegrambot.dispatcher.*
import com.github.kotlintelegrambot.entities.*
import java.io.File
import com.github.kotlintelegrambot.entities.TelegramFile

object UserMessageCalendar {
    const val calendarCaption = "Сохраняем в календарь, не стесняемся 😎"
}

class ScenarioCalendar(
    bot: Bot,
    dbUtils: DBUtils
): Scenario(bot, dbUtils) {
    private val calendarEvent = TelegramFile.ByFile(File("res/wed.ics"))

    override fun handleCommand(msg: Message) {
        val chatId = msg.chat.id
        if (chatIsAuthorized(chatId)) {
            bot.sendDocument(
                chatId = ChatId.fromId(chatId),
                document = calendarEvent,
                caption = UserMessageCalendar.calendarCaption
            )
        }
    }
}
