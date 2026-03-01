package wedbot.domain.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import wedbot.domain.entity.Drink
import wedbot.domain.entity.FoodInfo
import wedbot.domain.entity.Menu
import wedbot.domain.entity.Role
import wedbot.domain.entity.UserInfo
import wedbot.fakes.FakeTextRepository
import wedbot.fakes.FakeUserRepository
import wedbot.presentation.util.QueryData

class HandleFoodUseCaseTest {
    private val user = UserInfo(id = 1, chatId = 1L, role = Role.GUEST)
    private val userWithFood = UserInfo(
        id = 2, 
        chatId = 2L, 
        role = Role.GUEST, 
        foodInfo = FoodInfo(menuChoice = Menu.HROOHROO, drinks = setOf(Drink.WHITE, Drink.WHISKEY), additional = "Allergy")
    )

    @Test
    fun `invoke Ping - для нового пользователя должен предложить выбор горячего`() {
        val userRepository = FakeUserRepository(listOf(user))
        val textRepository = FakeTextRepository()
        val useCase = HandleFoodUseCase(userRepository, textRepository)
        val expected = HandleFoodUseCase.Result.Send("foodSurveyMenuPrompt", listOf(
            QueryData("foodSurveyMenuHroohroo", "food_hroohroo"),
            QueryData("foodSurveyMenuReebok", "food_reebok"),
            QueryData("foodSurveyMenuVegi", "food_vegi")
        ))
        val result = useCase.invoke(user.chatId!!, HandleFoodUseCase.Input.Ping)
        assertEquals(expected, result)
    }

    @Test
    fun `invoke Ping - для пользователя с данными должен вернуть резюме`() {
        val userRepository = FakeUserRepository(listOf(userWithFood))
        val textRepository = FakeTextRepository()
        val useCase = HandleFoodUseCase(userRepository, textRepository)
        val expected = HandleFoodUseCase.Result.Send("foodSurveySummary")
        val result = useCase.invoke(userWithFood.chatId!!, HandleFoodUseCase.Input.Ping)
        assertEquals(expected, result)
    }

    @Test
    fun `Полный опрос - последовательный выбор должен привести к сохранению в БД`() {
        val userRepository = FakeUserRepository(listOf(user))
        val textRepository = FakeTextRepository()
        val useCase = HandleFoodUseCase(userRepository, textRepository)
        val chatId = user.chatId!!
        // 1. Выбор меню
        val menuExpected = HandleFoodUseCase.Result.Edit(
            "foodSurveyDrinkPrompt",
            listOf(
                QueryData("foodSurveyDrinkWhite", "drink_white"),
                QueryData("foodSurveyDrinkRed", "drink_red"),
                QueryData("foodSurveyDrinkShampoo", "drink_shampoo"),
                QueryData("foodSurveyDrinkWhiskey", "drink_whiskey"),
                QueryData("foodSurveyDrinkVodka", "drink_vodka"),
                QueryData("foodSurveyDrinkAlcoholess", "drink_alcoholess"),
                QueryData("foodSurveyDoneButton", "drink_done")
            )
        )
        val menuRes = useCase.invoke(chatId, HandleFoodUseCase.Input.MenuQuery("food_reebok"))
        assertEquals(menuExpected, menuRes)
        // 2. Выбор нескольких напитков
        var drinkExpected = HandleFoodUseCase.Result.Edit(
            "foodSurveyDrinkPrompt",
            listOf(
                QueryData("foodSurveyDrinkWhite", "drink_white"),
                QueryData("✅ foodSurveyDrinkRed", "drink_red"),
                QueryData("foodSurveyDrinkShampoo", "drink_shampoo"),
                QueryData("foodSurveyDrinkWhiskey", "drink_whiskey"),
                QueryData("foodSurveyDrinkVodka", "drink_vodka"),
                QueryData("foodSurveyDrinkAlcoholess", "drink_alcoholess"),
                QueryData("foodSurveyDoneButton", "drink_done")
            )
        )
        var drinkRes = useCase.invoke(chatId, HandleFoodUseCase.Input.DrinkQuery("drink_red"))
        assertEquals(drinkExpected, drinkRes)
        drinkExpected = HandleFoodUseCase.Result.Edit(
            "foodSurveyDrinkPrompt",
            listOf(
                QueryData("foodSurveyDrinkWhite", "drink_white"),
                QueryData("✅ foodSurveyDrinkRed", "drink_red"),
                QueryData("foodSurveyDrinkShampoo", "drink_shampoo"),
                QueryData("foodSurveyDrinkWhiskey", "drink_whiskey"),
                QueryData("✅ foodSurveyDrinkVodka", "drink_vodka"),
                QueryData("foodSurveyDrinkAlcoholess", "drink_alcoholess"),
                QueryData("foodSurveyDoneButton", "drink_done")
            )
        )
        drinkRes = useCase.invoke(chatId, HandleFoodUseCase.Input.DrinkQuery("drink_vodka"))
        assertEquals(drinkExpected, drinkRes)
        // 3. Завершение выбора напитков
        val doneExpected = HandleFoodUseCase.Result.Edit(
            "foodSurveyAdditionalPrompt",
            listOf(
                QueryData("foodSurveySkipAdditional", "food_skip_additional")
            )
        )
        val doneRes = useCase.invoke(chatId, HandleFoodUseCase.Input.DrinkQuery("drink_done"))
        assertEquals(doneExpected, doneRes)
        assertTrue(useCase.isUserInEnteringMode(chatId))
        // 4. Ввод доп информации и финиш
        val additionalText = "Deeez nuts"
        val finishExpected = HandleFoodUseCase.Result.Finished("foodSurveyFinished")
        val finishRes = useCase.invoke(chatId, HandleFoodUseCase.Input.AdditionalInfo(additionalText))
        assertEquals(finishExpected, finishRes)
        assertFalse(useCase.isUserInEnteringMode(chatId))
        // Проверка в БД
        val updatedUser = userRepository.getByChatId(chatId).getOrNull()
        val expectedFoodInfo = FoodInfo(
            menuChoice = Menu.REEBOK,
            drinks = setOf(Drink.RED, Drink.VODKA),
            additional = additionalText
        )
        assertEquals(expectedFoodInfo, updatedUser?.foodInfo)
    }

    @Test
    fun `Полный опрос - пропуск ввода доп информации должен привести к сохранению в БД`() {
        val userRepository = FakeUserRepository(listOf(user))
        val textRepository = FakeTextRepository()
        val useCase = HandleFoodUseCase(userRepository, textRepository)
        val chatId = user.chatId!!
        // 1. Выбор меню
        val menuExpected = HandleFoodUseCase.Result.Edit(
            "foodSurveyDrinkPrompt",
            listOf(
                QueryData("foodSurveyDrinkWhite", "drink_white"),
                QueryData("foodSurveyDrinkRed", "drink_red"),
                QueryData("foodSurveyDrinkShampoo", "drink_shampoo"),
                QueryData("foodSurveyDrinkWhiskey", "drink_whiskey"),
                QueryData("foodSurveyDrinkVodka", "drink_vodka"),
                QueryData("foodSurveyDrinkAlcoholess", "drink_alcoholess"),
                QueryData("foodSurveyDoneButton", "drink_done")
            )
        )
        val menuRes = useCase.invoke(chatId, HandleFoodUseCase.Input.MenuQuery("food_vegi"))
        assertEquals(menuExpected, menuRes)
        // 2. Выбор напитка
        val drinkExpected = HandleFoodUseCase.Result.Edit(
            "foodSurveyDrinkPrompt",
            listOf(
                QueryData("foodSurveyDrinkWhite", "drink_white"),
                QueryData("foodSurveyDrinkRed", "drink_red"),
                QueryData("foodSurveyDrinkShampoo", "drink_shampoo"),
                QueryData("foodSurveyDrinkWhiskey", "drink_whiskey"),
                QueryData("foodSurveyDrinkVodka", "drink_vodka"),
                QueryData("✅ foodSurveyDrinkAlcoholess", "drink_alcoholess"),
                QueryData("foodSurveyDoneButton", "drink_done")
            )
        )
        val drinkRes = useCase.invoke(chatId, HandleFoodUseCase.Input.DrinkQuery("drink_alcoholess"))
        assertEquals(drinkExpected, drinkRes)
        // 3. Завершение выбора напитков
        val doneExpected = HandleFoodUseCase.Result.Edit(
            "foodSurveyAdditionalPrompt",
            listOf(
                QueryData("foodSurveySkipAdditional", "food_skip_additional")
            )
        )
        val doneRes = useCase.invoke(chatId, HandleFoodUseCase.Input.DrinkQuery("drink_done"))
        assertEquals(doneExpected, doneRes)
        assertTrue(useCase.isUserInEnteringMode(chatId))
        // 4. Пропуск ввода доп информации
        val finishExpected = HandleFoodUseCase.Result.Finished("foodSurveyFinished")
        val finishRes = useCase.invoke(chatId, HandleFoodUseCase.Input.AdditionalInfo(""))
        assertEquals(finishExpected, finishRes)
        assertFalse(useCase.isUserInEnteringMode(chatId))
        // Проверка в БД
        val updatedUser = userRepository.getByChatId(chatId).getOrNull()
        val expectedFoodInfo = FoodInfo(
            menuChoice = Menu.VEGI,
            drinks = setOf(Drink.ALCOHOLESS)
        )
        assertEquals(expectedFoodInfo, updatedUser?.foodInfo)
    }

    @Test
    fun `invoke DrinkQuery - повторный выбор напитка должен снять галочку`() {
        val userRepository = FakeUserRepository(listOf(user))
        val textRepository = FakeTextRepository()
        val useCase = HandleFoodUseCase(userRepository, textRepository)
        val chatId = user.chatId!!
        useCase.invoke(chatId, HandleFoodUseCase.Input.MenuQuery("food_vegi"))
        val callback = "drink_shampoo"
        useCase.invoke(chatId, HandleFoodUseCase.Input.DrinkQuery(callback))
        val res = useCase.invoke(chatId, HandleFoodUseCase.Input.DrinkQuery(callback))
        val expected = HandleFoodUseCase.Result.Edit(
            "foodSurveyDrinkPrompt",
            listOf(
                QueryData("foodSurveyDrinkWhite", "drink_white"),
                QueryData("foodSurveyDrinkRed", "drink_red"),
                QueryData("foodSurveyDrinkShampoo", "drink_shampoo"),
                QueryData("foodSurveyDrinkWhiskey", "drink_whiskey"),
                QueryData("foodSurveyDrinkVodka", "drink_vodka"),
                QueryData("foodSurveyDrinkAlcoholess", "drink_alcoholess"),
                QueryData("foodSurveyDoneButton", "drink_done")
            )
        )
        assertEquals(expected, res)
    }

    @Test
    fun `invoke - должен вернуть Error, если пользователь не найден`() {
        val userRepository = FakeUserRepository(emptyList())
        val textRepository = FakeTextRepository()
        val useCase = HandleFoodUseCase(userRepository, textRepository)
        val expected = HandleFoodUseCase.Result.Error("userNotFound")
        val result = useCase.invoke(999L, HandleFoodUseCase.Input.Ping)
        assertEquals(expected, result)
    }
}
