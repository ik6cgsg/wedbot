package wedbot.domain.usecase

import wedbot.domain.entity.Role
import wedbot.domain.entity.UserInfo
import wedbot.domain.entity.toUserStatus
import wedbot.fakes.FakeTextRepository
import wedbot.fakes.FakeUserRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class VerifyPhoneUseCaseTest {
    private val userByPhone = UserInfo(id = 1, chatId = null, phone = "79991234567", role = Role.GUEST)
    private val userWithSamePhoneAndChatId = UserInfo(id = 2, chatId = 123L, phone = "79997654321", role = Role.GUEST)

    @Test
    fun `invoke - должен найти пользователя по телефону и обновить chatId & username`() {
        val userRepository = FakeUserRepository(listOf(userByPhone))
        val textRepository = FakeTextRepository()
        val useCase = VerifyPhoneUseCase(userRepository, textRepository)
        val newChatId = 456L
        val newUserName = "newuser"
        val expected = VerifyPhoneUseCase.Result.UserFound(
            "generateGreeting",
            userByPhone.copy(chatId = newChatId, username = newUserName).toUserStatus()!!
        )
        val result = useCase.invoke(newChatId, userByPhone.phone!!, newUserName)
        assertEquals(expected, result)
    }

    @Test
    fun `invoke - должен вернуть ошибку, если пользователь не найден`() {
        val userRepository = FakeUserRepository(listOf(userByPhone))
        val textRepository = FakeTextRepository()
        val useCase = VerifyPhoneUseCase(userRepository, textRepository)
        val expected = VerifyPhoneUseCase.Result.Error("userNotFound")
        val result = useCase.invoke(123L, "79990000000", null)
        assertEquals(expected, result)
    }

    @Test
    fun `invoke - должен вернуть ошибку, если телефон уже занят другим chatId`() {
        val userRepository = FakeUserRepository(listOf(userWithSamePhoneAndChatId))
        val textRepository = FakeTextRepository()
        val useCase = VerifyPhoneUseCase(userRepository, textRepository)
        val newChatIdTryingToStealPhone = 999L
        val expected = VerifyPhoneUseCase.Result.Error("alreadyRegistered")
        val result = useCase.invoke(newChatIdTryingToStealPhone, userWithSamePhoneAndChatId.phone!!, "other")
        assertEquals(expected, result)
    }
}
