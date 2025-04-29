package wedbot

import com.github.kotlintelegrambot.*
import com.github.kotlintelegrambot.dispatcher.*
import com.github.kotlintelegrambot.entities.*
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import java.io.File
import com.github.kotlintelegrambot.entities.TelegramFile

object UserMessageAuth {
    const val alreadyChatted = "А мы уже знакомы! Список команд можешь увидеть в меню ниже"
    const val notFound = "Кажется, я тебя не узнал, поделись пожалуйста контактом!"
    const val shareContact = "Поделиться контактом 🥺"
    const val invitationCaption = "Лови открытку от нас 🥺🫶"

    private val greetingTemplate = """
    Здравствуй, дорог%s %s%s!
    Рады сообщить, что приглашаем Тебя на наш праздник, который будет состоять из двух этапов:
    1. ЗАГС (опционально)
    2. Праздник жизни (musthave)
    """

    fun generateGreeting(sex: Sex, name: String?, nik: String?) = greetingTemplate.format(
        if (sex == Sex.FEMALE) "ая" else "ой",
        name ?: "пользователь без тг имени",
        if (nik?.isNotBlank() == true) ", более известный как `$nik` xDD" else ""
    )
}

class ScenarioAuth(
    bot: Bot,
    dbUtils: DBUtils
): Scenario(bot, dbUtils) {
    private val invitationPic = TelegramFile.ByFile(File("res/zagz.png"))

    override fun handleCommand(msg: Message) {
        val chatId = msg.chat.id
        val username = msg.from?.username
        var needPhoneCheck = false
        var userInfo = dbUtils.getUserByChatId(chatId)
        if (userInfo != null) { // already chatted
            bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = UserMessageAuth.alreadyChatted
            )
        } else if (username == null) { // user got no username (boomer ??)
            needPhoneCheck = true
        } else {
            userInfo = dbUtils.getUserByUsername(username)
            if (userInfo != null) { // first time chatting, send greeting and pic
                sendGreetingGroup(ChatId.fromId(chatId), userInfo)
                userInfo.chatId = chatId
                dbUtils.updateUser(userInfo)
            } else { // hmmm need phone check
                needPhoneCheck = true
            }
        }
        if (needPhoneCheck) {
            bot.sendMessage(
                chatId = ChatId.fromId(chatId),
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
            // TODO: other text??
            sendInternalError(chatId)
        }
    }

    private fun sendGreetingGroup(chatId: ChatId, userInfo: UserInfo) {
        bot.sendMessage(
            chatId = chatId,
            text = UserMessageAuth.generateGreeting(userInfo.sex, userInfo.realName, userInfo.nikName),
            replyMarkup = ReplyKeyboardRemove()
        )
        bot.sendPhoto(
            chatId = chatId,
            photo = invitationPic,
            caption = UserMessageAuth.invitationCaption
        )
    }

    private fun shareContactButton(): List<List<KeyboardButton>> {
        return listOf(
            listOf(KeyboardButton(UserMessageAuth.shareContact, requestContact = true)),
        )
    }
}
