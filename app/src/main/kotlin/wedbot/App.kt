package wedbot

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.*
import com.github.kotlintelegrambot.logging.LogLevel
import com.github.kotlintelegrambot.entities.*
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import com.github.kotlintelegrambot.entities.TelegramFile.ByFile
import com.github.kotlintelegrambot.entities.inputmedia.MediaGroup
import com.github.kotlintelegrambot.entities.inputmedia.InputMediaPhoto
import java.io.File
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun shareContactButton(): List<List<KeyboardButton>> {
    return listOf(
        listOf(KeyboardButton("Поделиться контактом", requestContact = true)),
    )
}

fun main() {
    val dbUtils = DBUtils()
    val bot = bot {
        //logLevel = LogLevel.All()
        token = System.getProperty("bot.token")
        dispatch {
            command("start") {
                val chatId = ChatId.fromId(message.chat.id)
                val username = message.from!!.username!! // TODO: throw handle null
                println("username = $username")
                val rr = dbUtils.getUserByUsername(username)
                println("db result = $rr")
                if (rr == null) {
                    bot.sendMessage(
                        chatId = chatId,
                        text = "Кажется, я Вас не узнал, поделитесь пожалуйста контактом!",
                        replyMarkup = KeyboardReplyMarkup(keyboard = shareContactButton())
                    )
                } else {
                    // TODO: this update failed
                    dbUtils.updateChatId(rr[Users.id], message.chat.id)
                    bot.sendMessage(
                        chatId = chatId,
                        text = "Здарова, ${rr[Users.realName]} aka ${rr[Users.nikName]}",
                        replyMarkup = ReplyKeyboardRemove()
                    )
                }
            }

            command("pic") {
                bot.sendPhoto(
                    chatId = ChatId.fromId(message.chat.id),
                    photo = TelegramFile.ByFile(File("res/swaga.jpg")),
                    caption = "Swaga"
                )
            }

            command("ics") {
                bot.sendDocument(
                    chatId = ChatId.fromId(message.chat.id),
                    document = TelegramFile.ByFile(File("res/wed.ics")),
                    caption = "Сохраняем в календарь, не стесняемся 😎"
                )
            }

            text("ping") {
                bot.sendMessage(chatId = ChatId.fromId(message.chat.id), text = "Pong")
            }

            contact {
                bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id),
                    text = "Hello, ${contact.firstName} ${contact.lastName}",
                    replyMarkup = ReplyKeyboardRemove()
                )
            }

            telegramError {
                println(error.getErrorMessage())
            }
        }
    }
    bot.startPolling()
}
