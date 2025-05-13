package wedbot

import com.github.kotlintelegrambot.*
import com.github.kotlintelegrambot.dispatcher.*
import com.github.kotlintelegrambot.entities.*
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.entities.ParseMode
import kotlin.text.trimMargin

object UserMessageAdmin {
    const val tbd = "Тo Be Developed"
    const val pingFormat = """
    Сообщение не отправлено, ожидаемый формат:
    
    `admin\_ping\_users <text\>`
    """
    val unknownCommand = """
    А ты точно ||админ||?
    Текущие команды:

    ⁕ `admin\_statuses` – возвращает список пользователей со статусом
    ⁕ `admin\_invites` – возвращает список заявок на ивайт
    ⁕ `admin\_ping\_users <text\>` – отправляет _text_ всем пользователям
    """.trimIndent()

    val statusTableHeader = """
    📊 Таблица статусов, стр %d
    """.trimIndent()

    val adminMessage = "⚠️ Сообщение от администрации ⚠️\n\n"
}

object TextCommandAdmin {
    val prefix = "admin"
    val adminUserStatuses = "${prefix}_statuses"
    val adminInvites = "${prefix}_invites"
    val adminSendTextToAllUsers = "${prefix}_ping_users"
}

object QueryAdmin {
    val nextStatusPage = "${TextCommandAdmin.prefix}NextStatusPage"
    val prevStatusPage = "${TextCommandAdmin.prefix}PrevStatusPage"
}

class ScenarioAdmin(
    bot: Bot,
    dbUtils: DBUtils
): Scenario(bot, dbUtils) {
    data class MetaInfo(var page: Int, val batchFunc: (Long, Int, Long?) -> Long?, var msgId: Long? = null) {
        fun batch(chatId: Long) {
            msgId = batchFunc(chatId, page, msgId)
        }
    }

    val limit = 10
    private val chatToMeta = mutableMapOf<Long, MetaInfo>()

    override fun handleText(text: String, chatId: Long) {
        if (dbUtils.getUserByChatId(chatId)?.role == Role.ADMIN) {
            if (text.startsWith(TextCommandAdmin.adminUserStatuses)) {
                chatToMeta[chatId]?.msgId?.let { msgId -> bot.deleteMessage(ChatId.fromId(chatId), msgId) }
                chatToMeta[chatId] = MetaInfo(0, ::sendBatchOfStatuses)
                chatToMeta[chatId]?.batch(chatId)
            } else if (text.startsWith(TextCommandAdmin.adminInvites)) {
                chatToMeta[chatId]?.msgId?.let { msgId -> bot.deleteMessage(ChatId.fromId(chatId), msgId) }
                chatToMeta[chatId] = MetaInfo(0, ::sendBatchOfInvites)
                chatToMeta[chatId]?.batch(chatId)
            } else if (text.startsWith(TextCommandAdmin.adminSendTextToAllUsers)) {
                sendTextToAllUsers(text, chatId)
            } else {
                bot.sendMessage(
                    chatId = ChatId.fromId(chatId),
                    text = UserMessageAdmin.unknownCommand,
                    parseMode = ParseMode.MARKDOWN_V2
                )
            }
        }
    }

    override fun handleQuery(query: CallbackQuery) {
        val chatId = query.message?.chat?.id ?: return
        val args = query.data.split(" ")
        when (args[0]) {
            QueryAdmin.nextStatusPage -> chatToMeta[chatId]?.let { 
                it.page += 1
                it.batch(chatId)
            }
            QueryAdmin.prevStatusPage -> chatToMeta[chatId]?.let {
                if (it.page > 0) {
                    it.page -= 1
                }
                it.batch(chatId)
            }
        }
    }

    private fun sendBatchOfStatuses(chatId: Long, page: Int, msgId: Long?): Long? {
        val offset = page * limit
        val userStatuses = dbUtils.getUserStatuses(offset.toLong(), limit)
        var rows = mutableListOf<List<String>>()
        userStatuses.forEach {
            rows.add(listOf<String>("${it.chatId}", "${it.username}", "${it.phone}", "${it.realName}", it.status.name))
        }
        var tableMsg = UserMessageAdmin.statusTableHeader.format(page) + "\n" + drawTextTable(
            headers = listOf("chatId", "username", "phone", "realName", "status"), rows
        )
        tableMsg = "```\n$tableMsg\n```"
        return sendTableMessage(chatId, msgId, tableMsg)
    }

    private fun sendBatchOfInvites(chatId: Long, page: Int, msgId: Long?): Long? {
        val offset = page * limit
        val inviteStatuses = dbUtils.getInviteStatuses(offset.toLong(), limit)
        var rows = mutableListOf<List<String>>()
        inviteStatuses.forEach {
            rows.add(listOf<String>("${it.id}", "${it.initiatorСhatId}", "${it.invitedСhatId}", "${it.userConfirmed}", "${it.adminConfirmed}", "${it.isCompleted}"))
        }
        var tableMsg = UserMessageAdmin.statusTableHeader.format(page) + "\n" + drawTextTable(
            headers = listOf("id", "initiator", "invited", "user", "admin", "completed"), rows
        )
        tableMsg = "```\n$tableMsg\n```"
        return sendTableMessage(chatId, msgId, tableMsg)
    }

    private fun sendTableMessage(chatId: Long, msgId: Long?, tableMsg: String): Long? {
        val tgChatId = ChatId.fromId(chatId)
        var resMsgId = msgId
        if (resMsgId == null) {
            val tgRes = bot.sendMessage(tgChatId,
                text = tableMsg,
                replyMarkup = generateStatusesKeyboard(),
                parseMode = ParseMode.MARKDOWN
            )
            tgRes.fold(ifSuccess = { msg ->
                resMsgId = msg.messageId
            }, ifError = {})
        } else {
            bot.editMessageText(tgChatId,
                messageId = resMsgId,
                text = tableMsg,
                replyMarkup = generateStatusesKeyboard(),
                parseMode = ParseMode.MARKDOWN
            )
        }
        return resMsgId
    }

    private fun generateStatusesKeyboard() = InlineKeyboardMarkup.create(listOf(listOf(
        InlineKeyboardButton.CallbackData("⬅️ Назад", "${QueryAdmin.prevStatusPage}"),
        InlineKeyboardButton.CallbackData("Вперёд ➡️", "${QueryAdmin.nextStatusPage}")
    )))

    private fun drawTextTable(
        headers: List<String>,
        rows: List<List<String>>,
        columnPadding: Int = 1
    ): String {
        // Calculate column widths
        val columnWidths = headers.mapIndexed { index, header ->
            maxOf(
                header.length,
                rows.maxOfOrNull { row -> row[index].length } ?: 0
            ) + columnPadding * 2
        }
        // Build horizontal line
        val horizontalLine = "+" + columnWidths.joinToString("+") { "-".repeat(it) } + "+"
        // Build header row
        val headerRow = "|" + headers.mapIndexed { index, header ->
            header.center(columnWidths[index])
        }.joinToString("|") + "|"
        // Build data rows
        val dataRows = rows.joinToString("\n") { row ->
            "|" + row.mapIndexed { index, cell ->
                cell.center(columnWidths[index])
            }.joinToString("|") + "|"
        }
        // Combine all parts
        return listOf(
            horizontalLine,
            headerRow,
            horizontalLine,
            dataRows,
            horizontalLine
        ).joinToString("\n")
    }

    private fun String.center(width: Int, padChar: Char = ' '): String {
        if (this.length >= width) return this
        val padding = width - this.length
        val leftPadding = padding / 2
        val rightPadding = padding - leftPadding
        return padChar.toString().repeat(leftPadding) + this + padChar.toString().repeat(rightPadding)
    }

    private fun sendTextToAllUsers(text: String, chatId: Long) {
        val cmdLen = TextCommandAdmin.adminSendTextToAllUsers.length
        if (text.length > cmdLen + 1) {
            dbUtils.getAllUserChats().forEach {
                bot.sendMessage(
                    chatId = ChatId.fromId(it),
                    text = UserMessageAdmin.adminMessage + text.substring(cmdLen + 1)
                )
            }
        } else {
            bot.sendMessage(ChatId.fromId(chatId), UserMessageAdmin.pingFormat,
                parseMode = ParseMode.MARKDOWN_V2)
        }
    }
}
