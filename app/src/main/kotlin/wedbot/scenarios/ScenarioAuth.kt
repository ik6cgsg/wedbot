package wedbot

import com.github.kotlintelegrambot.*
import com.github.kotlintelegrambot.dispatcher.*
import com.github.kotlintelegrambot.entities.*
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import java.io.File
import com.github.kotlintelegrambot.entities.TelegramFile
import com.github.kotlintelegrambot.entities.ParseMode

object UserMessageAuth {
    const val notFound = "Кажется, я тебя не узнал, поделись пожалуйста контактом!"
    const val notFoundTotal = "Извиняемся, вас нет в нашей базе 😕"
    const val shareContact = "Поделиться контактом"
    const val invitationCaption = "Лови открытку от нас 🥺🫶"

    private val greetingTemplate = """
    *Здравствуй, дорог%s %s*%s\!\!

    Рады сообщить, что приглашаем Тебя на наш праздник, который будет состоять из двух этапов:
    1\. ЗАГС \(опционально\)
    2\. Праздник жизни \(musthave\)
    """.trimIndent()

    fun generateGreeting(sex: Sex, name: String?, nik: String?) = greetingTemplate.format(
        if (sex == Sex.FEMALE) "ая" else "ой",
        name ?: "безымянный пользователь",
        if (nik?.isNotBlank() == true) ", более известный как ||_${nik}_|| xDD" else ""
    )

    fun generateCommandDescription(): String {
        val cmdList = Command.entries.map { "• /${it.cmd} – ${it.description}" }.joinToString(separator = "\n")
        return """
        Небольшое руководство: слева в поле ввода сообщения можно увидеть меню нашего бота с классными командами
        
$cmdList
        """.trimIndent()
    }
}

class ScenarioAuth(
    bot: Bot,
    dbUtils: DBUtils
): Scenario(bot, dbUtils) {
    private val invitationPic = TelegramFile.ByFile(File("res/zagz.png"))

    override fun handleCommand(msg: Message) {
        val chatId = msg.chat.id
        val chatIdTg = ChatId.fromId(chatId)
        val username = msg.from?.username
        var needPhoneCheck = false
        var userInfo = dbUtils.getUserByChatId(chatId)
        if (userInfo != null) { // already chatted, send greeting and pic
            sendGreetingGroup(chatIdTg, userInfo)
        } else if (username == null) { // user got no username (boomer ??)
            needPhoneCheck = true
        } else {
            userInfo = dbUtils.getUserByUsername(username)
            if (userInfo != null) { // first time chatting, send greeting and pic
                sendGreetingGroup(chatIdTg, userInfo)
                userInfo.chatId = chatId
                dbUtils.updateUser(userInfo)
            } else { // hmmm need phone check
                needPhoneCheck = true
            }
        }
        if (needPhoneCheck) {
            bot.sendMessage(
                chatId = chatIdTg,
                text = UserMessageAuth.notFound,
                replyMarkup = KeyboardReplyMarkup(keyboard = shareContactButton(), resizeKeyboard = true)
            )
        }
    }

    fun handleContact(chatId: Long, phone: String, username: String?) {
        val userInfo = dbUtils.getUserByPhone(phone)
        if (userInfo != null) { // ok, no username (or updated one?), but found by phone
            sendGreetingGroup(ChatId.fromId(chatId), userInfo)
            userInfo.chatId = chatId
            userInfo.username = username
            dbUtils.updateUser(userInfo)
        } else { // hmmm user not in db 
            bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = UserMessageAuth.notFoundTotal,
                replyMarkup = ReplyKeyboardRemove()
            )
        }
    }

    private fun sendGreetingGroup(chatId: ChatId, userInfo: UserInfo) {
        bot.sendMessage(
            chatId = chatId,
            text = UserMessageAuth.generateGreeting(userInfo.sex, userInfo.realName, userInfo.nikName),
            replyMarkup = ReplyKeyboardRemove(),
            parseMode = ParseMode.MARKDOWN_V2
        )
        // bot.sendPhoto(
        //     chatId = chatId,
        //     photo = invitationPic,
        //     caption = UserMessageAuth.invitationCaption,
        // )
        bot.sendMessage(
            chatId = chatId,
            text = UserMessageAuth.generateCommandDescription(),
            parseMode = ParseMode.MARKDOWN
        )
    }

    private fun shareContactButton(): List<List<KeyboardButton>> {
        return listOf(
            listOf(KeyboardButton(UserMessageAuth.shareContact, requestContact = true)),
        )
    }
}
