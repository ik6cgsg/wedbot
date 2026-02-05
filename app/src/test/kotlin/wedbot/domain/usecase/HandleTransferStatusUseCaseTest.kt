package wedbot.domain.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import wedbot.domain.entity.Role
import wedbot.domain.entity.TransferStatus
import wedbot.domain.entity.UserInfo
import wedbot.fakes.FakeTextRepository
import wedbot.fakes.FakeUserRepository
import wedbot.presentation.util.QueryData

class HandleTransferStatusUseCaseTest {
    private val userThinking = UserInfo(id = 1, chatId = 1L, role = Role.GUEST, transferStatus = TransferStatus.THINKING)
    private val userNeed = UserInfo(id = 2, chatId = 2L, role = Role.GUEST, transferStatus = TransferStatus.NEED)
    private val userSelf = UserInfo(id = 3, chatId = 3L, role = Role.GUEST, transferStatus = TransferStatus.SELF_HANDLE)
    private val userLegend = UserInfo(id = 4, chatId = 4L, role = Role.GUEST, transferStatus = TransferStatus.SOCIAL_LEGEND)

    @Test
    fun `invoke Query NEED - должен обновить статус на NEED и вернуть Edit`() {
        val userRepository = FakeUserRepository(listOf(userThinking))
        val textRepository = FakeTextRepository()
        val useCase = HandleTransferStatusUseCase(userRepository, textRepository)
        val input = HandleTransferStatusUseCase.Input.Query("transfer_status_need")
        val expected = HandleTransferStatusUseCase.Result.Edit("transferStatusNeedChoice")
        val result = useCase.invoke(userThinking.chatId!!, input)
        assertEquals(expected, result)
        val updatedUser = userRepository.getByChatId(userThinking.chatId).getOrNull()
        assertEquals(TransferStatus.NEED, updatedUser?.transferStatus)
    }

    @Test
    fun `invoke Query SELF_HANDLE - должен обновить статус на SELF_HANDLE и вернуть Edit`() {
        val userRepository = FakeUserRepository(listOf(userThinking))
        val textRepository = FakeTextRepository()
        val useCase = HandleTransferStatusUseCase(userRepository, textRepository)
        val input = HandleTransferStatusUseCase.Input.Query("transfer_status_self_handle")
        val expected = HandleTransferStatusUseCase.Result.Edit("transferStatusSelfHandleChoice")
        val result = useCase.invoke(userThinking.chatId!!, input)
        assertEquals(expected, result)
        val updatedUser = userRepository.getByChatId(userThinking.chatId).getOrNull()
        assertEquals(TransferStatus.SELF_HANDLE, updatedUser?.transferStatus)
    }

    @Test
    fun `invoke Query SOCIAL_LEGEND - должен обновить статус на SOCIAL_LEGEND и вернуть Edit`() {
        val userRepository = FakeUserRepository(listOf(userThinking))
        val textRepository = FakeTextRepository()
        val useCase = HandleTransferStatusUseCase(userRepository, textRepository)
        val input = HandleTransferStatusUseCase.Input.Query("transfer_status_social_legend")
        val expected = HandleTransferStatusUseCase.Result.Edit("transferStatusSocialLegendChoice")
        val result = useCase.invoke(userThinking.chatId!!, input)
        assertEquals(expected, result)
        val updatedUser = userRepository.getByChatId(userThinking.chatId).getOrNull()
        assertEquals(TransferStatus.SOCIAL_LEGEND, updatedUser?.transferStatus)
    }

    @Test
    fun `invoke Query THINKING - должен вернуть DeleteWithAlert`() {
        val userRepository = FakeUserRepository(listOf(userThinking))
        val textRepository = FakeTextRepository()
        val useCase = HandleTransferStatusUseCase(userRepository, textRepository)
        val input = HandleTransferStatusUseCase.Input.Query("transfer_status_thinking")
        val expected = HandleTransferStatusUseCase.Result.DeleteWithAlert("statusThinkAgain")
        val result = useCase.invoke(userThinking.chatId!!, input)
        assertEquals(expected, result)
        val updatedUser = userRepository.getByChatId(userThinking.chatId).getOrNull()
        assertEquals(TransferStatus.THINKING, updatedUser?.transferStatus)
    }

    @Test
    fun `invoke Query - должен вернуть Error, если статус уже установлен`() {
        val userRepository = FakeUserRepository(listOf(userNeed))
        val textRepository = FakeTextRepository()
        val useCase = HandleTransferStatusUseCase(userRepository, textRepository)
        val input = HandleTransferStatusUseCase.Input.Query("transfer_status_need")
        val expected = HandleTransferStatusUseCase.Result.Error("internalError")
        val result = useCase.invoke(userNeed.chatId!!, input)
        assertEquals(expected, result)
    }
    
    @Test
    fun `invoke Ping для THINKING - должен вернуть Send с кнопками`() {
        val userRepository = FakeUserRepository(listOf(userThinking))
        val textRepository = FakeTextRepository()
        val useCase = HandleTransferStatusUseCase(userRepository, textRepository)
        val input = HandleTransferStatusUseCase.Input.Ping
        val result = useCase.invoke(userThinking.chatId!!, input)
        val expected = HandleTransferStatusUseCase.Result.Send(
            "transferStatusPingThinking",
            buttonList = listOf(
                QueryData("transferStatusThinkingButton", "transfer_status_thinking"),
                QueryData("transferStatusNeedButton", "transfer_status_need"),
                QueryData("transferStatusSelfHandleButton", "transfer_status_self_handle"),
                QueryData("transferStatusSocialLegendButton", "transfer_status_social_legend"),
            )
        )
        assertEquals(expected, result)
    }

    @Test
    fun `invoke Ping для NEED - должен вернуть Send с текстом`() {
        val userRepository = FakeUserRepository(listOf(userNeed))
        val textRepository = FakeTextRepository()
        val useCase = HandleTransferStatusUseCase(userRepository, textRepository)
        val input = HandleTransferStatusUseCase.Input.Ping
        val expected = HandleTransferStatusUseCase.Result.Send("transferStatusPingNeed")
        val result = useCase.invoke(userNeed.chatId!!, input)
        assertEquals(expected, result)
    }

    @Test
    fun `invoke Ping для SELF_HANDLE - должен вернуть Send с текстом`() {
        val userRepository = FakeUserRepository(listOf(userSelf))
        val textRepository = FakeTextRepository()
        val useCase = HandleTransferStatusUseCase(userRepository, textRepository)
        val input = HandleTransferStatusUseCase.Input.Ping
        val expected = HandleTransferStatusUseCase.Result.Send("transferStatusPingSelfHandle")
        val result = useCase.invoke(userSelf.chatId!!, input)
        assertEquals(expected, result)
    }

    @Test
    fun `invoke Ping для SOCIAL_LEGEND - должен вернуть Send с текстом`() {
        val userRepository = FakeUserRepository(listOf(userLegend))
        val textRepository = FakeTextRepository()
        val useCase = HandleTransferStatusUseCase(userRepository, textRepository)
        val input = HandleTransferStatusUseCase.Input.Ping
        val expected = HandleTransferStatusUseCase.Result.Send("transferStatusPingSocialLegend")
        val result = useCase.invoke(userLegend.chatId!!, input)
        assertEquals(expected, result)
    }
    
    @Test
    fun `invoke - должен вернуть Error, если пользователь не найден`() {
        val userRepository = FakeUserRepository()
        val textRepository = FakeTextRepository()
        val useCase = HandleTransferStatusUseCase(userRepository, textRepository)
        val input = HandleTransferStatusUseCase.Input.Query("transfer_status_need")
        val expected = HandleTransferStatusUseCase.Result.Error("userNotFound")
        val result = useCase.invoke(123L, input)
        assertEquals(expected, result)
    }
}
