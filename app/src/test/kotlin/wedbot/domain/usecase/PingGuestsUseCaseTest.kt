package wedbot.domain.usecase

import wedbot.domain.entity.Role
import wedbot.domain.entity.Status
import wedbot.domain.entity.UserInfo
import wedbot.fakes.FakeTextRepository
import wedbot.fakes.FakeUserRepository
import wedbot.presentation.util.QueryData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PingGuestsUseCaseTest {
    private val admin = UserInfo(id = 1, chatId = 1L, role = Role.ADMIN)
    private val guest1 = UserInfo(id = 2, chatId = 2L, role = Role.GUEST, eventStatus = Status.APPROVED)
    private val guest2 = UserInfo(id = 3, chatId = 3L, role = Role.GUEST, eventStatus = Status.SLEEVE)

    private val textRepo = FakeTextRepository()

    @Test
    fun `invoke(Start) - должен разрешать рассылку для администратора`() {
        val userRepository = FakeUserRepository(listOf(admin))
        val useCase = PingGuestsUseCase(userRepository, textRepo)
        val expected = PingGuestsUseCase.Result.Start(
            "adminPingStarted",
            "adminPingAllPrompt",
            listOf(
                QueryData("adminPingGuestsButton", PingGuestsUseCase.QueryCommand.SWITCH_TO_GUESTS.id),
                QueryData("adminPingCancelButton", PingGuestsUseCase.QueryCommand.CANCEL.id)
            )
        )
        val result = useCase.invoke(admin.chatId!!, PingGuestsUseCase.Input.Start)
        assertEquals(expected, result)
    }

    @Test
    fun `invoke(Start) - должен запрещать рассылку для гостя`() {
        val userRepository = FakeUserRepository(listOf(guest1))
        val useCase = PingGuestsUseCase(userRepository, textRepo)
        val expected = PingGuestsUseCase.Result.Error("weakRights")
        val result = useCase.invoke(guest1.chatId!!, PingGuestsUseCase.Input.Start)
        assertEquals(expected, result)
    }

    @Test
    fun `invoke(SendText) - должен уведомить всех пользователей (по умолчанию)`() {
        val userRepository = FakeUserRepository(listOf(admin, guest1, guest2))
        val useCase = PingGuestsUseCase(userRepository, textRepo)
        // Enter ping mode
        useCase.invoke(admin.chatId!!, PingGuestsUseCase.Input.Start)
        // Get recipients
        val result = useCase.invoke(admin.chatId, PingGuestsUseCase.Input.SendText)
        val expected = PingGuestsUseCase.Result.PingRecipients(
            listOf(guest1.chatId!!, guest2.chatId!!),
            "adminMessageHeader\n\n",
            "adminPingSucceed"
        )
        assertEquals(expected, result)
    }

    @Test
    fun `invoke(Query=SWITCH_TO_GUESTS) - должен переключить режим рассылки на гостей`() {
        val userRepository = FakeUserRepository(listOf(admin, guest1, guest2))
        val useCase = PingGuestsUseCase(userRepository, textRepo)
        // Enter ping mode
        useCase.invoke(admin.chatId!!, PingGuestsUseCase.Input.Start)
        // Switch to guests
        val queryResult = useCase.invoke(
            admin.chatId,
            PingGuestsUseCase.Input.Query(PingGuestsUseCase.QueryCommand.SWITCH_TO_GUESTS)
        )
        val queryExpected = PingGuestsUseCase.Result.UpdateQuery(
            "adminPingGuestsPrompt",
            listOf(
                QueryData("adminPingAllButton", PingGuestsUseCase.QueryCommand.SWITCH_TO_ALL.id),
                QueryData("adminPingCancelButton", PingGuestsUseCase.QueryCommand.CANCEL.id)
            )
        )
        assertEquals(queryExpected, queryResult)
        // Get recipients
        val recipientsResult = useCase.invoke(admin.chatId, PingGuestsUseCase.Input.SendText)
        val recipientsExpected = PingGuestsUseCase.Result.PingRecipients(
            listOf(guest1.chatId!!),
            "adminMessageHeader\n\n",
            "adminPingSucceed"
        )
        assertEquals(recipientsExpected, recipientsResult)
    }

    @Test
    fun `invoke(Query=SWITCH_TO_ALL) - должен переключить режим рассылки на всех участников`() {
        val userRepository = FakeUserRepository(listOf(admin, guest1, guest2))
        val useCase = PingGuestsUseCase(userRepository, textRepo)
        // Enter ping mode
        useCase.invoke(admin.chatId!!, PingGuestsUseCase.Input.Start)
        // Switch to guests
        val queryResult = useCase.invoke(
            admin.chatId,
            PingGuestsUseCase.Input.Query(PingGuestsUseCase.QueryCommand.SWITCH_TO_ALL)
        )
        val queryExpected = PingGuestsUseCase.Result.UpdateQuery(
            "adminPingAllPrompt",
            listOf(
                QueryData("adminPingGuestsButton", PingGuestsUseCase.QueryCommand.SWITCH_TO_GUESTS.id),
                QueryData("adminPingCancelButton", PingGuestsUseCase.QueryCommand.CANCEL.id)
            )
        )
        assertEquals(queryExpected, queryResult)
        // Get recipients
        val recipientsResult = useCase.invoke(admin.chatId, PingGuestsUseCase.Input.SendText)
        val recipientsExpected = PingGuestsUseCase.Result.PingRecipients(
            listOf(guest1.chatId!!, guest2.chatId!!),
            "adminMessageHeader\n\n",
            "adminPingSucceed"
        )
        assertEquals(recipientsExpected, recipientsResult)
    }

    @Test
    fun `invoke(Query=CANCEL) - должен выйти из режима рассылки`() {
        val userRepository = FakeUserRepository(listOf(admin))
        val useCase = PingGuestsUseCase(userRepository, textRepo)
        // Enter ping mode
        useCase.invoke(admin.chatId!!, PingGuestsUseCase.Input.Start)
        assertTrue(useCase.isUserInPingMode(admin.chatId))
        // Cancel
        val result = useCase.invoke(
            admin.chatId,
            PingGuestsUseCase.Input.Query(PingGuestsUseCase.QueryCommand.CANCEL)
        )
        val expected = PingGuestsUseCase.Result.UpdateQuery(
            "adminPingCancel",
            listOf()
        )
        assertEquals(expected, result)
        assertTrue(!useCase.isUserInPingMode(admin.chatId))
    }
}
