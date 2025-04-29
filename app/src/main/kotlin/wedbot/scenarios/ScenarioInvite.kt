package wedbot

import com.github.kotlintelegrambot.*
import com.github.kotlintelegrambot.dispatcher.*
import com.github.kotlintelegrambot.entities.*
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton

object UserMessageInvite {
    private val initiatorMessageTemplate = "перешли кенту пж эту ссылку - https://t.me/w3dDbot?start=%d"
    private val inviteMessageTemplate = "пользователь %s (%s) хочет добавить вас, похоже на правду?"
    private val adminNotificationTemplate = "пользователь %s (%s) инициировал добавление %s (%s), согласовано?"
    private val initiatorInvitedConfirmedTemplate = "пользователь %s успешно приглашен"
    private val initiatorInvitedRejectedTemplate = "админы не одобрили добавление %s"

    const val invitedAuthed = "уже авторизованы"
    const val initiatorNotAuthed = "к сожалению вас пригласил неавторизованный пользователь"
    const val waitingAdmin = "ждем подтверждения админов"
    const val invitedRejected = "вы отказались от приглашения"
    const val initiatorInvitedRejected = "пользователь отклонил ваше приглашение(("
    const val invitedAdminConfirmed = "вы успешно зарегестрированы на мероприятие"
    const val invitedAdminRejected = "админы не одобрили заявку"
    const val adminConfirmedForAdmins = "заявка успешно одобрена одним из админов"
    const val adminRejectedForAdmins = "заявка отклонена одним из админов"

    fun generateLinkMessage(chatId: Long) = initiatorMessageTemplate.format(chatId)
    fun generateInviteMessage(username: String?, name: String?) = inviteMessageTemplate.format(username, name)
    fun generateAdminNotification(initiatorUsername: String?, initiatorName: String?, invitedUsername: String?, invitedName: String?) =
        adminNotificationTemplate.format(initiatorUsername, initiatorName, invitedUsername, invitedName)
    fun generateInviteConfirmedMessage(username: String?) = initiatorInvitedConfirmedTemplate.format(username)
    fun generateInviteRejectedMessage(username: String?) = initiatorInvitedRejectedTemplate.format(username)
}

object QueryInvite {
    val inviteUserAccept = "${CommandName.invite}UserAccept"
    val inviteUserReject = "${CommandName.invite}UserReject"
    val inviteAdminAccept = "${CommandName.invite}AdminAccept"
    val inviteAdminReject = "${CommandName.invite}AdminReject"
}

class ScenarioInvite(
    bot: Bot,
    dbUtils: DBUtils
): Scenario(bot, dbUtils) {
    override fun handleCommand(msg: Message) {
        val chatId = msg.chat.id
        if (chatIsAuthorized(chatId)) {
            bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = UserMessageInvite.generateLinkMessage(chatId)
            )
        }
    }

    override fun handleQuery(query: CallbackQuery) {
        val chatId = query.message?.chat?.id ?: return
        val args = query.data.split(" ")
        when (args[0]) {
            QueryInvite.inviteUserAccept -> handleInvitedUserConfirmed(chatId, query.from.username, query.from.firstName)
            QueryInvite.inviteUserReject -> handleInvitedUserRejected(chatId)
            QueryInvite.inviteAdminAccept -> handleAdminConfirmedInvite(args[1].toInt())
            QueryInvite.inviteAdminReject -> handleAdminRejectedInvite(args[1].toInt())
        }
    }

    fun handleInvitedUserStart(curChatId: Long, initiatorChatIdStr: String) {
        val initiatorChatId = initiatorChatIdStr.toLongOrNull()
        val curChatIdTg = ChatId.fromId(curChatId)
        if (initiatorChatId != null) {
            val mayBeAuthedUser = dbUtils.getUserByChatId(curChatId)
            if (mayBeAuthedUser != null) {
                bot.sendMessage(curChatIdTg, UserMessageInvite.invitedAuthed)
            } else {
                var initiator = dbUtils.getUserByChatId(initiatorChatId)
                if (initiator == null) {
                    bot.sendMessage(curChatIdTg, UserMessageInvite.initiatorNotAuthed)
                } else {
                    dbUtils.createInviteEvent(initiatorChatId, curChatId)
                    bot.sendMessage(
                        chatId = curChatIdTg,
                        text = UserMessageInvite.generateInviteMessage(initiator.username, initiator.realName),
                        replyMarkup = InlineKeyboardMarkup.create(listOf(listOf(
                            InlineKeyboardButton.CallbackData("✅", QueryInvite.inviteUserAccept),
                            InlineKeyboardButton.CallbackData("❌", QueryInvite.inviteUserReject)
                        )))
                    )
                }
            }
        } else {
            sendInternalError(curChatId)
        }
    }

    private fun handleInvitedUserConfirmed(chatId: Long, username: String?, realName: String?) {
        val inviteEvent = dbUtils.getActiveInviteByChatId(chatId)
        val curChatIdTg = ChatId.fromId(chatId)
        if (inviteEvent != null) {
            bot.sendMessage(curChatIdTg, UserMessageInvite.waitingAdmin)
            inviteEvent.userConfirmed = true
            inviteEvent.invitedUsername = username
            inviteEvent.invitedRealName = realName
            dbUtils.updateInviteEvent(inviteEvent)
            val initiator = dbUtils.getUserByChatId(inviteEvent.initiatorСhatId)
            dbUtils.getAdminChats().forEach { adminId ->
                bot.sendMessage(
                    chatId = ChatId.fromId(adminId),
                    text = UserMessageInvite.generateAdminNotification(initiator?.username, initiator?.realName, username, realName),
                    replyMarkup = InlineKeyboardMarkup.create(listOf(listOf(
                        InlineKeyboardButton.CallbackData("✅", "${QueryInvite.inviteAdminAccept} ${inviteEvent.id}"),
                        InlineKeyboardButton.CallbackData("❌", "${QueryInvite.inviteAdminReject} ${inviteEvent.id}")
                    )))
                )
            }
        } else {
            sendInternalError(chatId)
        }
    }

    private fun handleInvitedUserRejected(chatId: Long) {
        dbUtils.getActiveInviteByChatId(chatId)?.let {
            bot.sendMessage(ChatId.fromId(chatId), UserMessageInvite.invitedRejected)
            it.userConfirmed = false
            it.isCompleted = true
            dbUtils.updateInviteEvent(it)
            bot.sendMessage(ChatId.fromId(it.initiatorСhatId), UserMessageInvite.initiatorInvitedRejected)
        }
    }

    private fun handleAdminConfirmedInvite(inviteId: Int) {
        val inviteEvent = dbUtils.getActiveInviteById(inviteId)
        if (inviteEvent != null) {
            inviteEvent.adminConfirmed = true
            inviteEvent.isCompleted = true
            dbUtils.updateInviteEvent(inviteEvent)
            dbUtils.createUser(inviteEvent.invitedСhatId, inviteEvent.invitedUsername, inviteEvent.invitedRealName)
            bot.sendMessage(
                chatId = ChatId.fromId(inviteEvent.invitedСhatId),
                text = UserMessageInvite.invitedAdminConfirmed
            )
            bot.sendMessage(
                chatId = ChatId.fromId(inviteEvent.initiatorСhatId),
                text = UserMessageInvite.generateInviteConfirmedMessage(inviteEvent.invitedUsername)
            )
            dbUtils.getAdminChats().forEach { adminId -> 
                bot.sendMessage(ChatId.fromId(adminId), UserMessageInvite.adminConfirmedForAdmins)
            }
        } else {
            dbUtils.getAdminChats().forEach { adminId -> 
                sendInternalError(adminId)
            }
        }
    }

    private fun handleAdminRejectedInvite(inviteId: Int) {
        val inviteEvent = dbUtils.getActiveInviteById(inviteId)
        if (inviteEvent != null) {
            inviteEvent.adminConfirmed = false
            inviteEvent.isCompleted = true
            dbUtils.updateInviteEvent(inviteEvent)
            bot.sendMessage(
                chatId = ChatId.fromId(inviteEvent.invitedСhatId),
                text = UserMessageInvite.invitedAdminRejected
            )
            bot.sendMessage(
                chatId = ChatId.fromId(inviteEvent.initiatorСhatId),
                text = UserMessageInvite.generateInviteRejectedMessage(inviteEvent.invitedUsername)
            )
            dbUtils.getAdminChats().forEach { adminId -> 
                bot.sendMessage(ChatId.fromId(adminId), UserMessageInvite.adminRejectedForAdmins)
            }
        } else {
            dbUtils.getAdminChats().forEach { adminId -> 
                sendInternalError(adminId)
            }
        }
    }
}
