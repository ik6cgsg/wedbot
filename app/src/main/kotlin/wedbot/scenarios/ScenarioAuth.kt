package wedbot

import com.github.kotlintelegrambot.*
import com.github.kotlintelegrambot.dispatcher.*
import com.github.kotlintelegrambot.entities.*
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import java.io.File
import com.github.kotlintelegrambot.entities.TelegramFile
import com.github.kotlintelegrambot.entities.ParseMode

object UserMessageAuth {
    const val notFound = "Кажется, я тебя не узнал, поделись, пожалуйста, контактом 🙏"
    const val notFoundTotal = "Извини, но кажется тебя нет в базе 😕"
    const val numberAlreadyUsed = "Этот номер уже используется 😕"
    const val shareContact = "Поделиться контактом"

    private val greetingTemplate = """
    *Здравствуй, %s %s*%s\!

    С радостью приглашаем тебя разделить с нами одно очень важное событие 🤍

    Проходить оно будет в два этапа, будем искренне ждать тебя на каждом:

    💍 __Торжественная регистрация__
          ⁕ прийти следует на *20 минут* пораньше
          ⁕ займёт не более *одного часа*
          ⁕ дресс\-кот официальный для впечатляющих фотокарточек
          ⁕ просьба приходить самостоятельно – без подарков\!
        
    💍 __Организованный праздник__
          ⁕ ориентир – весна *2026* года
          ⁕ _*пожалуйста, не удаляй*_ чат с ботом для дальнейшей связи
    """.trimIndent()

    private val descriptionTemplate = """
    Cлева от поля ввода можно увидеть меню нашего бота.
    Не забудь им воспользоваться! Вот небольшое руководство:

    %s
    """.trimIndent()

    fun generateGreeting(sex: Sex, name: String?, nik: String?) = greetingTemplate.format(
        when (sex) {
            Sex.FEMALE -> "дорогая"
            Sex.MALE -> "дорогой"
            Sex.NE_BYLO -> ""
        },
        name ?: "гость",
        if (nik?.isNotBlank() == true) {
            ", более известн${if (sex == Sex.FEMALE) "ая" else "ый"} как ||_${nik}_|| 🫰" 
        } else ""
    )

    fun generateCommandDescription(): String {
        val cmdList = Command.entries.map { "⁕ /${it.cmd} – ${it.description}" }.joinToString(separator = "\n")
        return descriptionTemplate.format(cmdList)
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
        val tgChatId = ChatId.fromId(chatId)
        val userInfo = dbUtils.getUserByPhone(phone)
        if (userInfo != null) { // ok, no username (or updated one?), but found by phone
            if (userInfo.chatId == null) {
                sendGreetingGroup(tgChatId, userInfo)
                userInfo.chatId = chatId
                userInfo.username = username
                dbUtils.updateUser(userInfo)
            } else { // [hack] user sent another's guest number
                bot.sendMessage(tgChatId,
                    text = UserMessageAuth.numberAlreadyUsed,
                    replyMarkup = ReplyKeyboardRemove()
                )
            }
        } else { // hmmm user not in db
            bot.sendMessage(tgChatId,
                text = UserMessageAuth.notFoundTotal,
                replyMarkup = ReplyKeyboardRemove()
            )
        }
    }

    private fun sendGreetingGroup(chatId: ChatId, userInfo: UserInfo) {
         bot.sendPhoto(
            chatId = chatId,
            photo = invitationPic,
            replyMarkup = ReplyKeyboardRemove()
        )
        bot.sendChatAction(chatId, ChatAction.TYPING)
        Thread.sleep(1000)
        bot.sendMessage(
            chatId = chatId,
            text = UserMessageAuth.generateGreeting(userInfo.sex, userInfo.realName, userInfo.nikName),
            parseMode = ParseMode.MARKDOWN_V2
        )
        bot.sendChatAction(chatId, ChatAction.TYPING)
        Thread.sleep(1000)
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
