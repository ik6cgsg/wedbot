package wedbot.domain.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import wedbot.domain.entity.Role
import wedbot.domain.entity.Status
import wedbot.domain.entity.UserInfo
import wedbot.fakes.FakeTextRepository
import wedbot.fakes.FakeUserRepository

class HandleEventStatusUseCaseTest {
    private val userThinking = UserInfo(id = 1, chatId = 1L, role = Role.GUEST, eventStatus = Status.THINKING)

    @Test
    fun `invoke - должен обновить статус на APPROVED и вернуть Edit`() {
        val userRepository = FakeUserRepository(listOf(userThinking))
        val textRepository = FakeTextRepository()
        val useCase = HandleEventStatusUseCase(userRepository, textRepository)
        val newStatus = Status.APPROVED
        val expected = HandleEventStatusUseCase.Result.Edit("eventStatusAccepted")
        val result = useCase.invoke(userThinking.chatId!!, newStatus)
        assertEquals(expected, result)
        val updatedUser = userRepository.getByChatId(userThinking.chatId).getOrNull()
        assertEquals(newStatus, updatedUser?.eventStatus)
    }

    @Test
    fun `invoke - должен обновить статус на SLEEVE и вернуть Edit`() {
        val userRepository = FakeUserRepository(listOf(userThinking))
        val textRepository = FakeTextRepository()
        val useCase = HandleEventStatusUseCase(userRepository, textRepository)
        val newStatus = Status.SLEEVE
        val expected = HandleEventStatusUseCase.Result.Edit("eventStatusRejected")
        val result = useCase.invoke(userThinking.chatId!!, newStatus)
        assertEquals(expected, result)
        val updatedUser = userRepository.getByChatId(userThinking.chatId).getOrNull()
        assertEquals(newStatus, updatedUser?.eventStatus)
    }

    @Test
    fun `invoke - должен вернуть Error, если пользователь не найден`() {
        val userRepository = FakeUserRepository(emptyList())
        val textRepository = FakeTextRepository()
        val useCase = HandleEventStatusUseCase(userRepository, textRepository)
        val nonExistentChatId = 999L
        val expected = HandleEventStatusUseCase.Result.Error("userNotFound")
        val result = useCase.invoke(nonExistentChatId, Status.APPROVED)
        assertEquals(expected, result)
    }
}
