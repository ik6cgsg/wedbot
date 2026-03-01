package wedbot.domain.usecase

import wedbot.domain.entity.FoodInfo
import wedbot.domain.entity.Role
import wedbot.domain.entity.Status
import wedbot.domain.entity.UserInfo
import wedbot.fakes.FakeTextRepository
import wedbot.fakes.FakeUserRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class MenuUseCaseTest {

    private val guestThinking = UserInfo(id = 1, chatId = 1L, role = Role.GUEST, eventStatus = Status.THINKING)
    private val guestApproved = UserInfo(id = 2, chatId = 2L, role = Role.GUEST, eventStatus = Status.APPROVED)
    private val guestApprovedVillaYes = UserInfo(id = 2, chatId = 2L, role = Role.GUEST, eventStatus = Status.APPROVED, villaStatus = Status.APPROVED)
    private val guestApprovedVillaNo = UserInfo(id = 2, chatId = 2L, role = Role.GUEST, eventStatus = Status.APPROVED, villaStatus = Status.SLEEVE)
    private val guestApprovedWithFood = UserInfo(id = 13, chatId = 18L, role = Role.GUEST, eventStatus = Status.APPROVED, foodInfo = FoodInfo())
    private val guestSleeve = UserInfo(id = 3, chatId = 3L, role = Role.GUEST, eventStatus = Status.SLEEVE)
    private val adminUser = UserInfo(id = 4, chatId = 4L, role = Role.ADMIN, eventStatus = Status.APPROVED)

    @Test
    fun `invoke для гостя THINKING - должен вернуть кнопки INFO, EVENT_STATUS и HELP`() {
        val userRepository = FakeUserRepository(listOf(guestThinking))
        val textRepository = FakeTextRepository()
        val useCase = MenuUseCase(userRepository, textRepository)
        val result = useCase.invoke(guestThinking.chatId!!)
        val expected = MenuUseCase.Result.Markup(
            "menuMessage",
            listOf(
                listOf("menuButtonInfo"),
                listOf("menuButtonEventStatus"),
                listOf("menuButtonHelp")
            )
        )
        assertEquals(expected, result)
    }

    @Test
    fun `invoke для гостя APPROVED - должен содержать логистику и опросы, но не EVENT_STATUS`() {
        val userRepository = FakeUserRepository(listOf(guestApproved))
        val textRepository = FakeTextRepository()
        val useCase = MenuUseCase(userRepository, textRepository)
        val result = useCase.invoke(guestApproved.chatId!!)
        val expected = MenuUseCase.Result.Markup(
            "menuMessage",
            listOf(
                listOf("menuButtonInfo", "menuButtonDressCode"),
                listOf("menuButtonIcs", "menuButtonLocation"),
                listOf("menuButtonVillaStatus ⚠️", "menuButtonTransfer ⚠️"),
                listOf("menuButtonFood ⚠️"),
                listOf("menuButtonHelp")
            )
        )
        assertEquals(expected, result)
    }

    @Test
    fun `invoke для гостя APPROVED + согл на коттедж - должен установить эмодзи статус`() {
        val userRepository = FakeUserRepository(listOf(guestApprovedVillaYes))
        val textRepository = FakeTextRepository()
        val useCase = MenuUseCase(userRepository, textRepository)
        val result = useCase.invoke(guestApprovedVillaYes.chatId!!)
        val expected = MenuUseCase.Result.Markup(
            "menuMessage",
            listOf(
                listOf("menuButtonInfo", "menuButtonDressCode"),
                listOf("menuButtonIcs", "menuButtonLocation"),
                listOf("menuButtonVillaStatus ✅", "menuButtonTransfer ⚠️"),
                listOf("menuButtonFood ⚠️"),
                listOf("menuButtonHelp")
            )
        )
        assertEquals(expected, result)
    }

    @Test
    fun `invoke для гостя APPROVED с непустым foodInfo - должен установить эмодзи статус на кнопку опроса`() {
        val userRepository = FakeUserRepository(listOf(guestApprovedWithFood))
        val textRepository = FakeTextRepository()
        val useCase = MenuUseCase(userRepository, textRepository)
        val result = useCase.invoke(guestApprovedWithFood.chatId!!)
        val expected = MenuUseCase.Result.Markup(
            "menuMessage",
            listOf(
                listOf("menuButtonInfo", "menuButtonDressCode"),
                listOf("menuButtonIcs", "menuButtonLocation"),
                listOf("menuButtonVillaStatus ⚠️", "menuButtonTransfer ⚠️"),
                listOf("menuButtonFood ✅"),
                listOf("menuButtonHelp")
            )
        )
        assertEquals(expected, result)
    }

    @Test
    fun `invoke для гостя APPROVED + слив с коттеджа - должен установить эмодзи статус`() {
        val userRepository = FakeUserRepository(listOf(guestApprovedVillaNo))
        val textRepository = FakeTextRepository()
        val useCase = MenuUseCase(userRepository, textRepository)
        val result = useCase.invoke(guestApprovedVillaNo.chatId!!)
        val expected = MenuUseCase.Result.Markup(
            "menuMessage",
            listOf(
                listOf("menuButtonInfo", "menuButtonDressCode"),
                listOf("menuButtonIcs", "menuButtonLocation"),
                listOf("menuButtonVillaStatus 🚫", "menuButtonTransfer ⚠️"),
                listOf("menuButtonFood ⚠️"),
                listOf("menuButtonHelp")
            )
        )
        assertEquals(expected, result)
    }

    @Test
    fun `invoke для гостя SLEEVE - должен вернуть только HELP и сообщение totalSleeve`() {
        val userRepository = FakeUserRepository(listOf(guestSleeve))
        val textRepository = FakeTextRepository()
        val useCase = MenuUseCase(userRepository, textRepository)

        val result = useCase.invoke(guestSleeve.chatId!!)
        val expected = MenuUseCase.Result.Markup(
            "totalSleeve",
            listOf(
                listOf("menuButtonHelp")
            )
        )
        assertEquals(expected, result)
    }

    @Test
    fun `invoke для ADMIN - должен вернуть ВСЕ кнопки кроме help`() {
        val userRepository = FakeUserRepository(listOf(adminUser))
        val textRepository = FakeTextRepository()
        val useCase = MenuUseCase(userRepository, textRepository)
        val result = useCase.invoke(adminUser.chatId!!)
        val expected = MenuUseCase.Result.Markup(
            "menuMessage",
            listOf(
                listOf("menuButtonInfo", "menuButtonDressCode"),
                listOf("menuButtonIcs", "menuButtonLocation"),
                listOf("menuButtonVillaStatus ⚠️", "menuButtonTransfer ⚠️"),
                listOf("menuButtonFood ⚠️"),
                listOf("menuButtonStatusTable"),
                listOf("menuButtonPingGuests")
            )
        )
        assertEquals(expected, result)
    }
}
