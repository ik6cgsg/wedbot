package wedbot

import com.github.kotlintelegrambot.*
import com.github.kotlintelegrambot.dispatcher.*
import com.github.kotlintelegrambot.entities.*
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.entities.dice.DiceEmoji
import com.github.kotlintelegrambot.logging.LogLevel
import com.github.kotlintelegrambot.extensions.filters.Filter
import kotlin.random.Random

class WedBot(
    private val dbUtils: DBUtils,
) {
    private val bot: Bot

    init {
        val debug = System.getProperty("debug", "false").toBoolean()
        bot = bot {
            if (debug) logLevel = LogLevel.All()
            token = System.getProperty("bot.token")
            dispatch {
                println(this@dispatch)
                // MARK: Dispatch authorization
                command(CommandName.start) {
                    val startTextArgs = message.text?.split(" ")
                    if (startTextArgs?.size == 2) {
                        handleInvitedUserStart(message.chat.id, message.from?.username, startTextArgs[1])
                    } else {
                        handleStart(message.chat.id, message.from?.username)
                    }
                }
                contact {
                    // TODO: check if during invite process
                    handleContact(message.chat.id, contact.phoneNumber, message.from?.username)
                }
                // MARK: Dispatch invite
                command(CommandName.invite) {
                    handleInviteUser(message.chat.id)
                }
                callbackQuery(CommandName.inviteUserAccept) {
                    val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
                    val username = callbackQuery.from.username
                    val realName = callbackQuery.from.firstName
                    handleInvitedUserConfirmed(chatId, username, realName)
                }
                callbackQuery(CommandName.inviteUserReject) {
                    val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
                    handleInvitedUserRejected(chatId)
                }
                // MARK: Dispatch status
                // TODO: poll??
                command(CommandName.changeStatus) {
                    handleChangeStatus(message.chat.id)
                }
                callbackQuery(CommandName.statusChangeAccept) {
                    val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
                    handleChangeStatusResult(chatId, Status.ACCEPT)
                }
                callbackQuery(CommandName.statusChangeReject) {
                    val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
                    handleChangeStatusResult(chatId, Status.REJECT)
                }
                callbackQuery(CommandName.statusChangeNotSure) {
                    val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
                    handleChangeStatusResult(chatId, Status.NOT_SURE)
                }
                // MARK: Dispatch other
                command(CommandName.saveCalendar) {
                    handleSaveCalendar(message.chat.id)
                }
                message(Filter.Sticker) {
                    bot.sendMessage(ChatId.fromId(message.chat.id), UserMessage.sticker)
                }
                text("dep") {
                    val cid = ChatId.fromId(message.chat.id)
                    val diceList = listOf(DiceEmoji.Football, DiceEmoji.SlotMachine, DiceEmoji.Bowling, DiceEmoji.Basketball, DiceEmoji.Dartboard, DiceEmoji.Dice)
                    val randomDice = diceList[Random.nextInt(diceList.size)]
                    bot.sendDice(cid, randomDice)
                }
                // MARL: Common handlers
                callbackQuery {
                    val args = callbackQuery.data.split(" ")
                    if (args.size == 2 && args[1].toIntOrNull() != null) {
                        when (args[0]) {
                            CommandName.inviteAdminAccept -> handleAdminConfirmedInvite(args[1].toInt())
                            CommandName.inviteAdminReject -> handleAdminRejectedInvite(args[1].toInt())
                        }
                    }
                }
                telegramError {
                    println(error.getErrorMessage())
                }
            }
        }
    }

    fun start() {
        // TODO: webhooking?
        bot.startPolling()
    }

    // MARK: Authorization process

    private fun handleStart(chatId: Long, username: String?) {
        var needPhoneCheck = false
        var userInfo = dbUtils.getUserByChatId(chatId)
        if (userInfo != null) { // already chatted
            bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = UserMessage.alreadyChatted
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
                text = UserMessage.notFound,
                replyMarkup = KeyboardReplyMarkup(keyboard = shareContactButton(), resizeKeyboard = true)
            )
        }
    }

    private fun handleContact(chatId: Long, phone: String, username: String?) {
        val userInfo = dbUtils.getUserByPhone(phone)
        if (userInfo != null) { // ok, no username (or updated one?), but found by phone
            sendGreetingGroup(ChatId.fromId(chatId), userInfo)
            userInfo.chatId = chatId
            userInfo.username = username
            dbUtils.updateUser(userInfo)
        } else { // hmmm user not in db 
            // TODO: other text??
            bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = UserMessage.internalError,
                replyMarkup = ReplyKeyboardRemove()
            )
        }
    }

    private fun sendGreetingGroup(chatId: ChatId, userInfo: UserInfo) {
        bot.sendMessage(
            chatId = chatId,
            text = UserMessage.generateGreeting(userInfo.sex, userInfo.realName, userInfo.nikName),
            replyMarkup = ReplyKeyboardRemove()
        )
        bot.sendPhoto(
            chatId = chatId,
            photo = Document.invitationPic,
            caption = UserMessage.invitationCaption
        )
    }

    // MARK: Invite process

    private fun handleInviteUser(chatId: Long) {
        if (chatIsAuthorized(chatId)) {
            bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                // TODO: format message
                text = "перешли кенту пж эту ссылку - https://t.me/w3dDbot?start=$chatId"
            )
        }
    }

    private fun handleInvitedUserStart(curChatId: Long, username: String?, initiatorChatIdStr: String) {
        val initiatorChatId = initiatorChatIdStr.toLongOrNull()
        if (initiatorChatId != null) {
            // TODO: check curChatId is authorized
            var initiator = dbUtils.getUserByChatId(initiatorChatId)
            if (initiator == null) {
                bot.sendMessage(
                    chatId = ChatId.fromId(curChatId),
                    text = "к сожалению вас пригласил неавторизованный пользователь"
                )
            } else {
                dbUtils.createInviteEvent(initiatorChatId, curChatId)
                bot.sendMessage(
                    chatId = ChatId.fromId(curChatId),
                    text = "пользователь ${initiator.username} (${initiator.realName}) хочет добавить вас, похоже на правду?",
                    replyMarkup = InlineKeyboardMarkup.create(listOf(listOf(
                        InlineKeyboardButton.CallbackData("✅", CommandName.inviteUserAccept),
                        InlineKeyboardButton.CallbackData("❌", CommandName.inviteUserReject)
                    )))
                )
            }
        } else {
            sendInternalError(curChatId)
        }
    }

    private fun handleInvitedUserConfirmed(chatId: Long, username: String?, realName: String?) {
        val inviteEvent = dbUtils.getActiveInvite(chatId)
        if (inviteEvent != null) {
            bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = "ждем подтверждения админов"
            )
            inviteEvent.userConfirmed = true
            inviteEvent.invitedUsername = username
            inviteEvent.invitedRealName = realName
            dbUtils.updateInviteEvent(inviteEvent)
            val initiator = dbUtils.getUserByChatId(inviteEvent.initiatorСhatId)
            dbUtils.getAdminChats().forEach { adminId ->
                bot.sendMessage(
                    chatId = ChatId.fromId(adminId),
                    text = "пользователь ${initiator} инициировал добавление $username ($realName)",
                    replyMarkup = InlineKeyboardMarkup.create(listOf(listOf(
                        InlineKeyboardButton.CallbackData("✅", "${CommandName.inviteAdminAccept} ${inviteEvent.id}"),
                        InlineKeyboardButton.CallbackData("❌", "${CommandName.inviteAdminReject} ${inviteEvent.id}")
                    )))
                )
            }
        } else {
            TODO()
        }
    }

    private fun handleInvitedUserRejected(chatId: Long) {
        bot.sendMessage(
            chatId = ChatId.fromId(chatId),
            text = "вы отказались от приглашения"
        )
        dbUtils.getActiveInvite(chatId)?.let {
            it.userConfirmed = false
            it.isCompleted = true
            dbUtils.updateInviteEvent(it)
            bot.sendMessage(
                chatId = ChatId.fromId(it.initiatorСhatId),
                text = "пользователь отклонил ваше приглашение(("
            )
        }
    }

    private fun handleAdminConfirmedInvite(inviteId: Int) {
        val inviteEvent = dbUtils.getInviteById(inviteId)
        if (inviteEvent != null) {
            inviteEvent.adminConfirmed = true
            if (inviteEvent.userConfirmed == true && inviteEvent.adminConfirmed == true) {
                inviteEvent.isCompleted = true
                dbUtils.createUser(inviteEvent.invitedСhatId, inviteEvent.invitedUsername, inviteEvent.invitedRealName)
                bot.sendMessage(
                    chatId = ChatId.fromId(inviteEvent.invitedСhatId),
                    text = "добро пожаловать!"
                )
                bot.sendMessage(
                    chatId = ChatId.fromId(inviteEvent.initiatorСhatId),
                    text = "пользователь ${inviteEvent.invitedUsername} успешно приглашен"
                )
            } else {
                TODO()
            }
            dbUtils.updateInviteEvent(inviteEvent)
        } else {
            TODO()
        }
    }

    private fun handleAdminRejectedInvite(inviteId: Int) {
        val inviteEvent = dbUtils.getInviteById(inviteId)
        if (inviteEvent != null) {
            inviteEvent.adminConfirmed = false
            inviteEvent.isCompleted = true
            dbUtils.updateInviteEvent(inviteEvent)
            bot.sendMessage(
                chatId = ChatId.fromId(inviteEvent.invitedСhatId),
                text = "админы не одобрили заявку"
            )
            bot.sendMessage(
                chatId = ChatId.fromId(inviteEvent.initiatorСhatId),
                text = "админы не одобрили добавление ${inviteEvent.invitedUsername}"
            )
        } else {
            TODO()
        }
    }

    // MARK: Change status process

    private fun handleChangeStatus(chatId: Long) {
        getUserIfAuthorized(chatId)?.let {
            bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = UserMessage.generateStatus(it.status),
                replyMarkup = InlineKeyboardMarkup.create(inlineStatusButtons())
            )
        }
    }

    private fun handleChangeStatusResult(chatId: Long, newStatus: Status) {
        getUserIfAuthorized(chatId)?.let {
            it.status = newStatus
            dbUtils.updateUser(it)
            bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = UserMessage.generateStatusResult(newStatus)
            )
        }
    }

    private fun handleSaveCalendar(chatId: Long) {
        if (chatIsAuthorized(chatId)) {
            bot.sendDocument(
                chatId = ChatId.fromId(chatId),
                document = Document.calendarEvent,
                caption = UserMessage.calendarCaption
            )
        }
    }

    // MARK: Utils

    private fun shareContactButton(): List<List<KeyboardButton>> {
        return listOf(
            listOf(KeyboardButton(KeyboardButtonText.shareContact, requestContact = true)),
        )
    }

    private fun inlineStatusButtons(): List<List<InlineKeyboardButton>> {
        return listOf(
            listOf(InlineKeyboardButton.CallbackData(
                UserMessage.statusMap[Status.ACCEPT]!!, CommandName.statusChangeAccept)),
            listOf(InlineKeyboardButton.CallbackData(
                UserMessage.statusMap[Status.REJECT]!!, CommandName.statusChangeReject)),
            listOf(InlineKeyboardButton.CallbackData(
                UserMessage.statusMap[Status.NOT_SURE]!!, CommandName.statusChangeNotSure))
        )
    }

    private fun getUserIfAuthorized(chatId: Long): UserInfo? {
        var userInfo = dbUtils.getUserByChatId(chatId)
        if (userInfo == null) {
            sendInternalError(chatId)
        }
        return userInfo
    }

    private fun chatIsAuthorized(chatId: Long): Boolean {
        return getUserIfAuthorized(chatId) != null
    }

    private fun sendInternalError(chatId: Long) {
        bot.sendMessage(
            chatId = ChatId.fromId(chatId),
            text = UserMessage.internalError
        )
    }
}
