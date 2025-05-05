package wedbot

import com.github.kotlintelegrambot.*
import com.github.kotlintelegrambot.dispatcher.*
import com.github.kotlintelegrambot.entities.*
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.entities.ParseMode
import kotlin.text.trimMargin

object UserMessageAdmin {
    val unknownCommand = """
    А вы точно ||админ||?
    Текущие команды:

    • `admin\_statuses` – возвращает список пользователей со статусом
    • `admin\_ping\_users` – отправляет сообщение всем пользователям
    • \[TBD\] `admin\_remind` – напоминает о событии всем подтвердишим пользователям
    """.trimIndent()

    val statusTableHeader = """
    📊 Таблица статусов, стр %d
    """.trimIndent()

    val adminMessage = "⚠️ Сообщение от администрации ⚠️\n\n"
}

object TextCommandAdmin {
    val prefix = "admin"
    val adminUserStatuses = "${prefix}_statuses"
    val adminSendTextToAllUsers = "${prefix}_ping_users"
    val adminRemindAcceptedUsers = "${prefix}_remind"
}

object QueryAdmin {
    val nextStatusPage = "${TextCommandAdmin.prefix}NextStatusPage"
    val prevStatusPage = "${TextCommandAdmin.prefix}PrevStatusPage"
}

class ScenarioAdmin(
    bot: Bot,
    dbUtils: DBUtils
): Scenario(bot, dbUtils) {
    data class PageAndId(var page: Int, var id: Long? = null)

    val limit = 10
    private val chatToPageID = mutableMapOf<Long, PageAndId>()

    override fun handleText(text: String, chatId: Long) {
        if (dbUtils.getUserByChatId(chatId)?.role == Role.ADMIN) {
            if (text.startsWith(TextCommandAdmin.adminUserStatuses)) {
                chatToPageID[chatId] = PageAndId(0)
                sendBatchOfStatuses(chatId)
            } else if (text.startsWith(TextCommandAdmin.adminSendTextToAllUsers)) {
                sendTextToAllUsers(text)
            } else if (text.startsWith(TextCommandAdmin.adminRemindAcceptedUsers)) {
                TODO()
            } else {
                bot.sendMessage(
                    chatId = ChatId.fromId(chatId),
                    text = UserMessageAdmin.unknownCommand,
                    parseMode = ParseMode.MARKDOWN_V2)
            }
        }
    }

    override fun handleQuery(query: CallbackQuery) {
        val chatId = query.message?.chat?.id ?: return
        val args = query.data.split(" ")
        when (args[0]) {
            QueryAdmin.nextStatusPage -> {
                chatToPageID[chatId]?.let { it.page += 1 }
                sendBatchOfStatuses(chatId)
            }
            QueryAdmin.prevStatusPage -> {
                chatToPageID[chatId]?.let {
                    if (it.page > 0) {
                        it.page -= 1
                    }
                }
                sendBatchOfStatuses(chatId)
            }
        }
    }

    private fun sendBatchOfStatuses(chatId: Long) {
        chatToPageID[chatId]?.let { pageId ->
            val page = pageId.page
            val offset = page * limit
            val userStatuses = dbUtils.getUserStatuses(offset.toLong(), limit)
            var rows = mutableListOf<List<String>>()
            userStatuses.forEach {
                rows.add(listOf<String>(it.username ?: "null", it.phone ?: "null", it.realName ?: "null", it.status.name))
            }
            var tableMsg = UserMessageAdmin.statusTableHeader.format(page) + "\n" + drawTextTable(
                headers = listOf("username", "phone", "realName", "status"), rows
            )
            tableMsg = "```\n$tableMsg\n```"
            if (pageId.id == null) {
                val tgRes = bot.sendMessage(
                    chatId = ChatId.fromId(chatId),
                    text = tableMsg,
                    replyMarkup = generateStatusesKeyboard(),
                    parseMode = ParseMode.MARKDOWN
                )
                tgRes.fold(ifSuccess = { msg ->
                    pageId.id = msg.messageId
                }, ifError = {})
            } else {
                bot.editMessageText(
                    chatId = ChatId.fromId(chatId),
                    messageId = pageId.id,
                    text = tableMsg,
                    replyMarkup = generateStatusesKeyboard(),
                    parseMode = ParseMode.MARKDOWN
                )
            }
        }
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

    private fun sendTextToAllUsers(text: String) {
        val cmdLen = TextCommandAdmin.adminSendTextToAllUsers.length
        dbUtils.getAllUserChats().forEach {
            bot.sendMessage(
                chatId = ChatId.fromId(it),
                text = UserMessageAdmin.adminMessage + text.substring(cmdLen + 1)
            )
        }
    }

    private fun remindAcceptedUsers() {

    }
}
