//package wedbot
//
//import com.github.kotlintelegrambot.*
//import com.github.kotlintelegrambot.dispatcher.*
//import com.github.kotlintelegrambot.entities.*
//import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
//import com.github.kotlintelegrambot.entities.ParseMode
//import com.github.kotlintelegrambot.types.getOrDefault
//import wedbot.data.db.DBUtils
//import kotlin.collections.arrayListOf
//
//object UserMessageInvite {
//    private val initiatorMessageTemplate = """
//    Перешли указанную ссылку тому, кого ты желаешь пригласить
//
//    [инвайт-ссылка](https://t.me/w3dDbot?start=%d)
//    """.trimIndent()
//    private val inviteMessageTemplate = "Пользователь %s (%s) хочет позвать вас на свадьбу Ильи и Анастасии, похоже на правду?"
//    private val adminNotificationTemplate = "Пользователь %s (%s) инициировал добавление %s (%s), согласовано?"
//    private val initiatorInvitedConfirmedTemplate = "Пользователь %s успешно приглашен"
//    private val initiatorInvitedRejectedTemplate = "Администрация не одобрила добавление %s"
//
//    const val invitedAuthed = "Ты уже приглашен! Пожалуйста, перезапусти бота с помощью /start"
//    const val invitedInProgress = "Сначала мы должны обработать текущее приглашение!"
//    const val initiatorNotAuthed = "К сожалению, приглашение невалидно"
//    const val waitingAdmin = "Ждем подтверждения администрации ⏳"
//    const val invitedRejected = "Приглашение отменено"
//    const val initiatorInvitedRejected = "Пользователь отклонил приглашение"
//    const val invitedAdminConfirmed = "Ура, ты с нами! Пожалуйста, перезапусти бота с помощью /start"
//    const val invitedAdminRejected = "Администрация не одобрила заявку"
//    const val adminConfirmedForAdmins = "Заявка #%d успешно одобрена одним из админов"
//    const val adminRejectedForAdmins = "Заявка #%d отклонена одним из админов"
//
//    fun generateLinkMessage(chatId: Long) = initiatorMessageTemplate.format(chatId)
//    fun generateInviteMessage(username: String?, name: String?) = inviteMessageTemplate.format(username, name)
//    fun generateAdminNotification(initiatorUsername: String?, initiatorName: String?, invitedUsername: String?, invitedName: String?) =
//        adminNotificationTemplate.format(initiatorUsername, initiatorName, invitedUsername, invitedName)
//    fun generateInviteConfirmedMessage(username: String?) = initiatorInvitedConfirmedTemplate.format(username)
//    fun generateInviteRejectedMessage(username: String?) = initiatorInvitedRejectedTemplate.format(username)
//}
//
//object QueryInvite {
//    val inviteUserAccept = "${Command.INVITE_GUEST.cmd}UserAccept"
//    val inviteUserReject = "${Command.INVITE_GUEST.cmd}UserReject"
//    val inviteAdminAccept = "${Command.INVITE_GUEST.cmd}AdminAccept"
//    val inviteAdminReject = "${Command.INVITE_GUEST.cmd}AdminReject"
//}
//
//class ScenarioInvite(
//    bot: Bot,
//    dbUtils: DBUtils
//): Scenario(bot, dbUtils) {
//    private var iventIdToAdminChatIdToMsgId = mutableMapOf<Int, MutableMap<Long, Long>>()
//
//    override fun handleCommand(msg: Message) {
//        val chatId = msg.chat.id
//        if (chatIsAuthorized(chatId)) {
//            bot.sendMessage(
//                chatId = ChatId.fromId(chatId),
//                text = UserMessageInvite.generateLinkMessage(chatId),
//                parseMode = ParseMode.MARKDOWN
//            )
//        }
//    }
//
//    override fun handleQuery(query: CallbackQuery) {
//        val chatId = query.message?.chat?.id ?: return
//        val args = query.data.split(" ")
//        when (args[0]) {
//            QueryInvite.inviteUserAccept -> handleInvitedUserConfirmed(chatId, query.from.username, query.from.firstName)
//            QueryInvite.inviteUserReject -> handleInvitedUserRejected(chatId)
//            QueryInvite.inviteAdminAccept -> handleAdminConfirmedInvite(args[1].toInt())
//            QueryInvite.inviteAdminReject -> handleAdminRejectedInvite(args[1].toInt())
//        }
//    }
//
//    fun handleInvitedUserStart(msg: Message) {
//        val args = msg.text?.split(" ") ?: return
//        val initiatorChatId = args[1].toLongOrNull()
//        val curChatId = msg.chat.id
//        val curChatIdTg = ChatId.fromId(curChatId)
//        if (initiatorChatId != null) {
//            if (dbUtils.getUserByChatId(curChatId) != null) {
//                bot.sendMessage(curChatIdTg, UserMessageInvite.invitedAuthed)
//            } else if (msg.from?.username != null && dbUtils.getUserByUsername(msg.from!!.username!!) != null) {
//                bot.sendMessage(curChatIdTg, UserMessageInvite.invitedAuthed)
//            } else if (dbUtils.getActiveInviteByChatId(curChatId)?.isCompleted == false) {
//                bot.sendMessage(curChatIdTg, UserMessageInvite.invitedInProgress)
//            } else {
//                var initiator = dbUtils.getUserByChatId(initiatorChatId)
//                if (initiator == null) {
//                    bot.sendMessage(curChatIdTg, UserMessageInvite.initiatorNotAuthed)
//                } else {
//                    dbUtils.createInviteEvent(initiatorChatId, curChatId)
//                    val tgRes = bot.sendMessage(
//                        chatId = curChatIdTg,
//                        text = UserMessageInvite.generateInviteMessage(initiator.username, initiator.realName),
//                        replyMarkup = InlineKeyboardMarkup.create(listOf(listOf(
//                            InlineKeyboardButton.CallbackData("✅", QueryInvite.inviteUserAccept),
//                            InlineKeyboardButton.CallbackData("❌", QueryInvite.inviteUserReject)
//                        )))
//                    )
//                    tgRes.fold(ifSuccess = { sentMsg ->
//                        chatToMsgId[curChatId] = sentMsg.messageId
//                    }, ifError = {})
//                }
//            }
//        } else {
//            sendInternalError(curChatId)
//        }
//    }
//
//    private fun handleInvitedUserConfirmed(chatId: Long, username: String?, realName: String?) {
//        val inviteEvent = dbUtils.getActiveInviteByChatId(chatId)
//        val curChatIdTg = ChatId.fromId(chatId)
//        if (inviteEvent != null) {
//            rmLastMessage(chatId)
//            queueMessageToRm(chatId, bot.sendMessage(curChatIdTg, UserMessageInvite.waitingAdmin))
//            inviteEvent.userConfirmed = true
//            inviteEvent.invitedUsername = username
//            inviteEvent.invitedRealName = realName
//            dbUtils.updateInviteEvent(inviteEvent)
//            val initiator = dbUtils.getUserByChatId(inviteEvent.initiatorСhatId)
//            iventIdToAdminChatIdToMsgId[inviteEvent.id] = mutableMapOf()
//            dbUtils.getAdminChats().forEach { adminId ->
//                val tgResAdmin = bot.sendMessage(
//                    chatId = ChatId.fromId(adminId),
//                    text = UserMessageInvite.generateAdminNotification(initiator?.username, initiator?.realName, username, realName),
//                    replyMarkup = InlineKeyboardMarkup.create(listOf(listOf(
//                        InlineKeyboardButton.CallbackData("✅", "${QueryInvite.inviteAdminAccept} ${inviteEvent.id}"),
//                        InlineKeyboardButton.CallbackData("❌", "${QueryInvite.inviteAdminReject} ${inviteEvent.id}")
//                    )))
//                )
//                tgResAdmin.fold(ifSuccess = { msg ->
//                    iventIdToAdminChatIdToMsgId[inviteEvent.id]?.put(adminId, msg.messageId)
//                }, ifError = {})
//            }
//        } else {
//            sendInternalError(chatId)
//        }
//    }
//
//    private fun handleInvitedUserRejected(chatId: Long) {
//        dbUtils.getActiveInviteByChatId(chatId)?.let {
//            bot.sendMessage(ChatId.fromId(chatId), UserMessageInvite.invitedRejected)
//            rmLastMessage(chatId)
//            it.userConfirmed = false
//            it.isCompleted = true
//            dbUtils.updateInviteEvent(it)
//            bot.sendMessage(ChatId.fromId(it.initiatorСhatId), UserMessageInvite.initiatorInvitedRejected)
//        }
//    }
//
//    private fun handleAdminConfirmedInvite(inviteId: Int) {
//        val inviteEvent = dbUtils.getActiveInviteById(inviteId)
//        if (inviteEvent != null) {
//            inviteEvent.adminConfirmed = true
//            inviteEvent.isCompleted = true
//            dbUtils.updateInviteEvent(inviteEvent)
//            dbUtils.createUser(inviteEvent.invitedСhatId, inviteEvent.invitedUsername, inviteEvent.invitedRealName)
//            bot.sendMessage(
//                chatId = ChatId.fromId(inviteEvent.invitedСhatId),
//                text = UserMessageInvite.invitedAdminConfirmed
//            )
//            rmLastMessage(inviteEvent.invitedСhatId)
//            bot.sendMessage(
//                chatId = ChatId.fromId(inviteEvent.initiatorСhatId),
//                text = UserMessageInvite.generateInviteConfirmedMessage(inviteEvent.invitedUsername)
//            )
//            dbUtils.getAdminChats().forEach { adminId ->
//                val adminIdTg = ChatId.fromId(adminId)
//                bot.sendMessage(adminIdTg, UserMessageInvite.adminConfirmedForAdmins.format(inviteId))
//                iventIdToAdminChatIdToMsgId[inviteId]?.get(adminId)?.let { msgId ->
//                    bot.deleteMessage(adminIdTg, msgId)
//                }
//                iventIdToAdminChatIdToMsgId[inviteId]?.remove(adminId)
//            }
//        } else {
//            dbUtils.getAdminChats().forEach { adminId ->
//                sendInternalError(adminId)
//            }
//        }
//    }
//
//    private fun handleAdminRejectedInvite(inviteId: Int) {
//        val inviteEvent = dbUtils.getActiveInviteById(inviteId)
//        if (inviteEvent != null) {
//            inviteEvent.adminConfirmed = false
//            inviteEvent.isCompleted = true
//            dbUtils.updateInviteEvent(inviteEvent)
//            bot.sendMessage(
//                chatId = ChatId.fromId(inviteEvent.invitedСhatId),
//                text = UserMessageInvite.invitedAdminRejected
//            )
//            rmLastMessage(inviteEvent.invitedСhatId)
//            bot.sendMessage(
//                chatId = ChatId.fromId(inviteEvent.initiatorСhatId),
//                text = UserMessageInvite.generateInviteRejectedMessage(inviteEvent.invitedUsername)
//            )
//            dbUtils.getAdminChats().forEach { adminId ->
//                val adminIdTg = ChatId.fromId(adminId)
//                bot.sendMessage(adminIdTg, UserMessageInvite.adminRejectedForAdmins.format(inviteId))
//                iventIdToAdminChatIdToMsgId[inviteId]?.get(adminId)?.let { msgId ->
//                    bot.deleteMessage(adminIdTg, msgId)
//                }
//                iventIdToAdminChatIdToMsgId[inviteId]?.remove(adminId)
//            }
//        } else {
//            dbUtils.getAdminChats().forEach { adminId ->
//                sendInternalError(adminId)
//            }
//        }
//    }
//}
