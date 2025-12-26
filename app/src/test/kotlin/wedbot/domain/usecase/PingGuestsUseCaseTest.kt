package wedbot.domain.usecase

import wedbot.domain.entity.Role
import wedbot.domain.entity.UserInfo
import wedbot.fakes.FakeTextRepository
import wedbot.fakes.FakeUserRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class PingGuestsUseCaseTest {
    private val admin = UserInfo(id = 1, chatId = 1L, role = Role.ADMIN)
    private val guest1 = UserInfo(id = 2, chatId = 2L, role = Role.GUEST)
    private val guest2 = UserInfo(id = 3, chatId = 3L, role = Role.GUEST)

    @Test
    fun `checkRights - должен разрешать рассылку для администратора`() {
        val userRepository = FakeUserRepository(listOf(admin))
        val useCase = PingGuestsUseCase(userRepository, FakeTextRepository())
        val expected = PingGuestsUseCase.CheckResult.Allowed(
            "adminPingStarted",
            "adminPingPrompt",
            "adminPingCancelButton")

        val result = useCase.checkRights(admin.chatId!!)
        assertEquals(expected, result)
    }

    @Test
    fun `checkRights - должен запрещать рассылку для гостя`() {
        val userRepository = FakeUserRepository(listOf(guest1))
        val useCase = PingGuestsUseCase(userRepository, FakeTextRepository())
        val expected = PingGuestsUseCase.CheckResult.Error("weakRights")
        val result = useCase.checkRights(guest1.chatId!!)
        assertEquals(expected, result)
    }

    @Test
    fun `getAllChats - должен возвращать всех пользователей, кроме самого себя`() {
        val userRepository = FakeUserRepository(listOf(admin, guest1, guest2))
        val useCase = PingGuestsUseCase(userRepository, FakeTextRepository())
        val chats = useCase.getAllChats(admin.chatId!!)
        assertEquals(2, chats.size)
        assertFalse(chats.contains(admin.chatId))
    }
}
