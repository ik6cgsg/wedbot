package wedbot.domain.usecase

import wedbot.domain.entity.Role
import wedbot.domain.entity.UserInfo
import wedbot.domain.entity.toUserStatus
import wedbot.fakes.FakeTextRepository
import wedbot.fakes.FakeUserRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class StartUseCaseTest {
    private val existingUserByChatId = UserInfo(id = 1, chatId = 1L, username = "testuser", role = Role.GUEST)
    private val existingUserByUsername = UserInfo(id = 2, chatId = null, username = "newuser", role = Role.GUEST)
    private val existingUserByPhone = UserInfo(id = 3, chatId = null, username = null, phone = "88005553535", role = Role.GUEST)

    @Test
    fun `invoke - должен найти существующего пользователя по chatId и вернуть UserFound`() {
        val userRepository = FakeUserRepository(listOf(existingUserByChatId))
        val textRepository = FakeTextRepository()
        val useCase = StartUseCase(userRepository, textRepository)
        val expected = StartUseCase.Result.UserFound(
            "generateGreeting",
            existingUserByChatId.toUserStatus()!!
        )
        val result = useCase.invoke(existingUserByChatId.chatId!!, existingUserByChatId.username)
        assertEquals(expected, result)
    }

    @Test
    fun `invoke - должен найти пользователя по username, обновить chatId и вернуть UserFound`() {
        val userRepository = FakeUserRepository(listOf(existingUserByUsername))
        val textRepository = FakeTextRepository()
        val useCase = StartUseCase(userRepository, textRepository)
        val newChatId = 123L
        val result = useCase.invoke(newChatId, existingUserByUsername.username)
        val expected = StartUseCase.Result.UserFound(
            "generateGreeting",
            existingUserByUsername.copy(chatId = newChatId).toUserStatus()!!
        )
        assertEquals(expected, result)
    }

    @Test
    fun `invoke - должен запросить телефон, если у пользователя нет chatId & username`() {
        val userRepository = FakeUserRepository(listOf(existingUserByPhone))
        val textRepository = FakeTextRepository()
        val useCase = StartUseCase(userRepository, textRepository)
        val newChatId = 9L
        val expected = StartUseCase.Result.NeedPhoneCheck(
            "shareContactError",
            "shareContactLabel"
        )
        val result = useCase.invoke(newChatId, null)
        assertEquals(expected, result)
    }

    @Test
    fun `invoke - должен запросить телефон, если у пользователя есть username (обновленный?), но был добавлен по телефону`() {
        val userRepository = FakeUserRepository(listOf(existingUserByPhone))
        val textRepository = FakeTextRepository()
        val useCase = StartUseCase(userRepository, textRepository)
        val newChatId = 7L
        val expected = StartUseCase.Result.NeedPhoneCheck(
            "shareContactError",
            "shareContactLabel"
        )
        val result = useCase.invoke(newChatId, "some_username")
        assertEquals(expected, result)
    }
}
