package wedbot

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.*
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.logging.LogLevel
import com.github.kotlintelegrambot.entities.ReplyKeyboardRemove
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import com.github.kotlintelegrambot.entities.TelegramFile
import com.github.kotlintelegrambot.entities.TelegramFile.ByFile
import com.github.kotlintelegrambot.entities.inputmedia.MediaGroup
import com.github.kotlintelegrambot.entities.inputmedia.InputMediaPhoto
import java.io.File
import wedbot.BotCreds

object BotCreds {
    val token = "7887769428:AAFfaGPKoN34Y6vzUg_0E4eYsjLAGWytW0o"
}

//data class App (val t: String = "Test") {}

fun generateUsersButton(): List<List<KeyboardButton>> {
    return listOf(
        listOf(KeyboardButton("Request contact", requestContact = true)),
        listOf(KeyboardButton("Gimme photo"))
    )
}

fun main() {
    val idFile = File("./id.txt")
    val bot = bot {
        logLevel = LogLevel.All()
        token = BotCreds.token
        dispatch {
            command("start") {
                idFile.writeText(message.chat.id.toString())
                bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id),
                    text = "Hello, ${message.from?.username} aka ${message.from?.firstName} ${message.from?.lastName}",
                    replyMarkup = KeyboardReplyMarkup(
                        keyboard = generateUsersButton(), resizeKeyboard = true
                    )
                )
            }
            
            text("ping") {
                bot.sendMessage(chatId = ChatId.fromId(message.chat.id), text = "Pong")
            }

            text("Gimme photo") {
                bot.sendMediaGroup(
                    chatId = ChatId.fromId(message.chat.id),
                    mediaGroup = MediaGroup.from(
                        InputMediaPhoto(
                            media = TelegramFile.ByFile(File("/Users/ilyakozlov/spbpu/wedbot/app/res/swaga.jpg")),
                            caption = "Swaga"
                        )
                    ),
                )
            }

            contact {
                bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id),
                    text = "Hello, ${contact.firstName} ${contact.lastName}",
                    //replyMarkup = ReplyKeyboardRemove()
                )
            }

            telegramError {
                println(error.getErrorMessage())
            }
        }
    }
    bot.startPolling()
    val chatId = idFile.readText()
    if (chatId.isNotBlank()) {
        println("chatId = $chatId")
        bot.sendMessage(chatId = ChatId.fromId(chatId.toLong()), text = "ne zhdali ???")
    }
}
