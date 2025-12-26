package wedbot.domain.usecase

import wedbot.domain.entity.Role
import wedbot.domain.entity.Status
import wedbot.domain.entity.UserInfo
import wedbot.fakes.FakeTextRepository
import wedbot.fakes.FakeUserRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class MenuUseCaseTest {
    private val adminUser = UserInfo(id = 1, chatId = 1L, role = Role.ADMIN, eventStatus = Status.APPROVED)
    private val approvedGuest = UserInfo(id = 2, chatId = 2L, role = Role.GUEST, eventStatus = Status.APPROVED)
    private val thinkingGuest = UserInfo(id = 3, chatId = 3L, role = Role.GUEST, eventStatus = Status.THINKING)
    private val rejectedGuest = UserInfo(id = 4, chatId = 4L, role = Role.GUEST, eventStatus = Status.SLEEVE)

    @Test
    fun `invoke - должен вернуть админское меню для администратора`() {
        val userRepository = FakeUserRepository(listOf(adminUser))
        val useCase = MenuUseCase(userRepository, FakeTextRepository())
        val expected = MenuUseCase.Result.Markup(
            "menuMessage",
            listOf(
                listOf("menuButtonInfo"),
                listOf("menuButtonStatusTable"),
                listOf("menuButtonPingGuests")
            )
        )
        val result = useCase.invoke(adminUser.chatId!!)
        assertEquals(expected, result)
    }

    @Test
    fun `invoke - должен вернуть меню для подтвержденного гостя`() {
        val userRepository = FakeUserRepository(listOf(approvedGuest))
        val useCase = MenuUseCase(userRepository, FakeTextRepository())
        val expected = MenuUseCase.Result.Markup(
            "menuMessage",
            listOf(
                listOf("menuButtonInfo"),
                listOf("menuButtonIcs", "menuButtonLocation"),
                listOf("menuButtonHelp")
            )
        )
        val result = useCase.invoke(approvedGuest.chatId!!)
        assertEquals(expected, result)
    }

    @Test
    fun `invoke - должен вернуть меню для думающего гостя`() {
        val userRepository = FakeUserRepository(listOf(thinkingGuest))
        val useCase = MenuUseCase(userRepository, FakeTextRepository())
        val expected = MenuUseCase.Result.Markup(
            "menuMessage",
            listOf(
                listOf("menuButtonInfo"),
                listOf("menuButtonEventStatus"),
                listOf("menuButtonHelp")
            )
        )
        val result = useCase.invoke(thinkingGuest.chatId!!)
        assertEquals(expected, result)
    }

    @Test
    fun `invoke - должен вернуть NoSuchUser для несуществующего пользователя`() {
        val userRepository = FakeUserRepository(emptyList())
        val useCase = MenuUseCase(userRepository, FakeTextRepository())
        val expected = MenuUseCase.Result.Error("userNotFound")
        val result = useCase.invoke(0L)
        assertEquals(expected, result)
    }

    @Test
    fun `invoke - должен вернуть ошибку для отказавшегося пользователя`() {
        val userRepository = FakeUserRepository(listOf(rejectedGuest))
        val useCase = MenuUseCase(userRepository, FakeTextRepository())
        val expected = MenuUseCase.Result.Error("weakRights")
        val result = useCase.invoke(4L)
        assertEquals(expected, result)
    }
}
