package wedbot

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.*
import com.github.kotlintelegrambot.types.TelegramBotResult
import io.mockk.*
import wedbot.data.db.DBUtils
import kotlin.test.*

class ScenarioTest {
    private val bot: Bot = mockk()
    private val dbUtils: DBUtils = mockk()
    private val scenario = object: Scenario(bot, dbUtils) {}
    private val message: Message = mockk()
    private val chat: Chat = mockk()
    private val chatIdLong = 30L
    private val chatIdId: ChatId.Id = mockk()
    private val tgRes: TelegramBotResult<Message> = mockk()

    @BeforeEach
    fun setup() {
        mockkObject(ChatId)
        mockkConstructor(ReplyKeyboardRemove::class)
        every { message.chat } returns chat
        every { chat.id } returns chatIdLong
        every { ChatId.fromId(chatIdLong) } returns chatIdId
        every { bot.sendMessage(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns tgRes
        every { bot.deleteMessage(any(), any()) } returns mockk()
    }

    @Test
    fun `test handleCommand`() {
        scenario.handleCommand(message)
        verify {
            bot.sendMessage(chatIdId, "Упс, что-то пошло не так, попробуй перезапустить бота (/start)",
                replyMarkup = any(ReplyKeyboardRemove::class))
        }
    }

    @Test
    fun `test handleQuery`() {
        val query: CallbackQuery = mockk()
        every { query.message } returns message
        scenario.handleQuery(query)
        verify {
            bot.sendMessage(
                chatId = chatIdId,
                text = "Упс, что-то пошло не так, попробуй перезапустить бота (/start)",
                replyMarkup = any(ReplyKeyboardRemove::class)
            )
        }
    }

    @Test
    fun `test handleText`() {
        scenario.handleText("test text", chatIdLong)
        verify {
            bot.sendMessage(
                chatId = chatIdId,
                text = "Упс, что-то пошло не так, попробуй перезапустить бота (/start)",
                replyMarkup = any(ReplyKeyboardRemove::class)
            )
        }
    }

    @Test
    fun `test getUserIfAuthorized when user is not authorized`() {
        every { dbUtils.getUserByChatId(chatIdLong) } returns null
        val userInfo = scenario.getUserIfAuthorized(chatIdLong)
        verify {
            bot.sendMessage(
                chatId = chatIdId,
                text = "Упс, что-то пошло не так, попробуй перезапустить бота (/start)",
                replyMarkup = any(ReplyKeyboardRemove::class)
            )
        }
        assertNull(userInfo)
    }

    @Test
    fun `test getUserIfAuthorized when user is authorized`() {
        val sigma = UserInfo(59)
        every { dbUtils.getUserByChatId(chatIdLong) } returns sigma
        val userInfo = scenario.getUserIfAuthorized(chatIdLong)
        verify { bot wasNot Called }
        assertEquals(sigma, userInfo)
    }

    @Test
    fun `test chatIsAuthorized when user is not authorized`() {
        every { dbUtils.getUserByChatId(chatIdLong) } returns null
        val isAuthorized = scenario.chatIsAuthorized(chatIdLong)
        verify {
            bot.sendMessage(
                chatId = chatIdId,
                text = "Упс, что-то пошло не так, попробуй перезапустить бота (/start)",
                replyMarkup = any(ReplyKeyboardRemove::class)
            )
        }
        assertFalse(isAuthorized)
    }

    @Test
    fun `test chatIsAuthorized when user is authorized`() {
        val sigma = UserInfo(59)
        every { dbUtils.getUserByChatId(chatIdLong) } returns sigma
        val isAuthorized = scenario.chatIsAuthorized(chatIdLong)
        verify { bot wasNot Called }
        assertTrue(isAuthorized)
    }

    @Test
    fun `test queueMessageToRm and rmLastMessage`() {
        val msgId = 456L
        every { message.messageId } returns msgId
        val tgRes: TelegramBotResult.Success<Message> = mockk()
        every { tgRes.value } returns message
        scenario.queueMessageToRm(chatIdLong, tgRes)
        assertEquals(msgId, scenario.chatToMsgId[chatIdLong])
        scenario.rmLastMessage(chatIdLong)
        verify {
            bot.deleteMessage(chatIdId, msgId)
        }
        assertEquals(null, scenario.chatToMsgId[chatIdLong])
    }
}
