package wedbot.domain.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import wedbot.domain.entity.Role
import wedbot.domain.entity.Status
import wedbot.domain.entity.UserInfo
import wedbot.fakes.FakeTextRepository
import wedbot.fakes.FakeUserRepository

class HandleVillaStatusUseCaseTest {
    private val userThinking = UserInfo(id = 1, chatId = 1L, role = Role.GUEST, villaStatus = Status.THINKING)
    private val userApproved = UserInfo(id = 2, chatId = 2L, role = Role.GUEST, villaStatus = Status.APPROVED)
    private val userRejected = UserInfo(id = 2, chatId = 2L, role = Role.GUEST, villaStatus = Status.SLEEVE)

    @Test
    fun `invoke Query ACCEPT - должен обновить статус на APPROVED и вернуть Edit`() {
        val userRepository = FakeUserRepository(listOf(userThinking))
        val textRepository = FakeTextRepository()
        val useCase = HandleVillaStatusUseCase(userRepository, textRepository)
        val expected = HandleVillaStatusUseCase.Result.Edit("villaStatusAccepted")
        val result = useCase.invoke(
            userThinking.chatId!!,
            HandleVillaStatusUseCase.Input.Query(HandleVillaStatusUseCase.QueryCommand.ACCEPT)
        )
        assertEquals(expected, result)
        val updatedUser = userRepository.getByChatId(userThinking.chatId).getOrNull()
        assertEquals(Status.APPROVED, updatedUser?.villaStatus)
    }

    @Test
    fun `invoke Query REJECT - должен обновить статус на SLEEVE и вернуть Edit`() {
        val userRepository = FakeUserRepository(listOf(userThinking))
        val textRepository = FakeTextRepository()
        val useCase = HandleVillaStatusUseCase(userRepository, textRepository)
        val expected = HandleVillaStatusUseCase.Result.Edit("villaStatusRejected")
        val result = useCase.invoke(
            userThinking.chatId!!,
            HandleVillaStatusUseCase.Input.Query(HandleVillaStatusUseCase.QueryCommand.REJECT)
        )
        assertEquals(expected, result)
        val updatedUser = userRepository.getByChatId(userThinking.chatId).getOrNull()
        assertEquals(Status.SLEEVE, updatedUser?.villaStatus)
    }

    @Test
    fun `invoke Query THINK - должен вернуть DeleteWithAlert`() {
        val userRepository = FakeUserRepository(listOf(userThinking))
        val textRepository = FakeTextRepository()
        val useCase = HandleVillaStatusUseCase(userRepository, textRepository)
        val expected = HandleVillaStatusUseCase.Result.DeleteWithAlert("statusThinkAgain")
        val result = useCase.invoke(
            userThinking.chatId!!,
            HandleVillaStatusUseCase.Input.Query(HandleVillaStatusUseCase.QueryCommand.THINK)
        )
        assertEquals(expected, result)
    }

    @Test
    fun `invoke Ping - должен вернуть Send для статуса THINKING`() {
        val userRepository = FakeUserRepository(listOf(userThinking))
        val textRepository = FakeTextRepository()
        val useCase = HandleVillaStatusUseCase(userRepository, textRepository)
        val result = useCase.invoke(userThinking.chatId!!, HandleVillaStatusUseCase.Input.Ping)
        assert(result is HandleVillaStatusUseCase.Result.Send)
        val sendResult = result as HandleVillaStatusUseCase.Result.Send
        assertEquals("villaStatusPingThinking", sendResult.text)
        assertEquals(3, sendResult.buttonList.size)
    }

    @Test
    fun `invoke Ping - должен вернуть Send для статуса APPROVED`() {
        val userRepository = FakeUserRepository(listOf(userApproved))
        val textRepository = FakeTextRepository()
        val useCase = HandleVillaStatusUseCase(userRepository, textRepository)
        val result = useCase.invoke(userApproved.chatId!!, HandleVillaStatusUseCase.Input.Ping)
        assert(result is HandleVillaStatusUseCase.Result.Send)
        val sendResult = result as HandleVillaStatusUseCase.Result.Send
        assertEquals("villaStatusPingApproved", sendResult.text)
        assertEquals(0, sendResult.buttonList.size)
    }

    @Test
    fun `invoke Ping - должен вернуть Send для статуса SLEEVE`() {
        val userRepository = FakeUserRepository(listOf(userRejected))
        val textRepository = FakeTextRepository()
        val useCase = HandleVillaStatusUseCase(userRepository, textRepository)
        val result = useCase.invoke(userRejected.chatId!!, HandleVillaStatusUseCase.Input.Ping)
        assert(result is HandleVillaStatusUseCase.Result.Send)
        val sendResult = result as HandleVillaStatusUseCase.Result.Send
        assertEquals("villaStatusPingRejected", sendResult.text)
        assertEquals(0, sendResult.buttonList.size)
    }

    @Test
    fun `invoke Query - должен вернуть Error, если статус уже установлен`() {
        val userRepository = FakeUserRepository(listOf(userApproved))
        val textRepository = FakeTextRepository()
        val useCase = HandleVillaStatusUseCase(userRepository, textRepository)
        val expected = HandleVillaStatusUseCase.Result.Error("internalError")
        val result = useCase.invoke(
            userApproved.chatId!!,
            HandleVillaStatusUseCase.Input.Query(HandleVillaStatusUseCase.QueryCommand.ACCEPT)
        )
        assertEquals(expected, result)
    }
}
