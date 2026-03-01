package wedbot.domain.usecase

import wedbot.domain.entity.Drink
import wedbot.domain.entity.FoodInfo
import wedbot.domain.entity.Menu
import wedbot.domain.repository.TextRepository
import wedbot.domain.repository.UserRepository
import wedbot.presentation.util.QueryData
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

class HandleFoodUseCase(
    private val userRepository: UserRepository,
    private val textRepository: TextRepository
) {
    sealed class Input {
        object Ping : Input()
        data class MenuQuery(val query: String) : Input()
        data class DrinkQuery(val query: String) : Input()
        data class AdditionalInfo(val text: String) : Input()
    }

    sealed class Result {
        data class Edit(
            val text: String,
            val buttonList: List<QueryData> = listOf()
        ) : Result()
        data class Send(
            val text: String,
            val buttonList: List<QueryData> = listOf()
        ) : Result()
        data class Error(val msg: String) : Result()
        data class Finished(val text: String) : Result()
    }

    private val callbackPrefixFood = "food_"
    private val callbackPrefixDrink = "drink_"
    private val callbackDrinkFinish = callbackPrefixDrink + "done"
    private val userStates = ConcurrentHashMap<Long, FoodInfo>()
    private val usersEnteringAdditional = Collections.synchronizedSet(mutableSetOf<Long>())

    val menuCallbacks = Menu.entries.map { it.callbackData }
    val drinkCallbacks = Drink.entries.map { it.callbackData } + callbackDrinkFinish
    val callbackSkipAdditional = callbackPrefixFood + "skip_additional"

    operator fun invoke(chatId: Long, input: Input): Result {
        return when (input) {
            is Input.Ping -> handlePing(chatId)
            is Input.MenuQuery -> {
                val choice = input.query.menu
                    ?: return Result.Error(textRepository.internalError())
                handleMenuSelect(chatId, choice)
            }
            is Input.DrinkQuery -> {
                if (input.query.startsWith(callbackPrefixDrink)) {
                    val choice = input.query.drink
                    if (choice != null) {
                        handleDrinkSelect(chatId, choice)
                    } else if (input.query == callbackDrinkFinish) {
                        handleDrinkFinish(chatId)
                    } else {
                        Result.Error(textRepository.internalError())
                    }
                } else {
                    Result.Error(textRepository.internalError())
                }
            }
            is Input.AdditionalInfo -> handleAdditionalInfo(chatId, input.text)
        }
    }

    fun isUserInEnteringMode(chatId: Long) = usersEnteringAdditional.contains(chatId)

    private fun handlePing(chatId: Long): Result {
        val user = userRepository.getByChatId(chatId).getOrElse {
            return Result.Error(textRepository.userNotFound())
        }
        return if (user.foodInfo != null) {
            val menuLabel = user.foodInfo.menuChoice.label
            val drinksLabel = user.foodInfo.drinks.joinToString(", ") { it.label }
            val additional = user.foodInfo.additional.ifBlank { "-" }
            Result.Send(textRepository.foodSurveySummary(menuLabel, drinksLabel, additional))
        } else {
            userStates.remove(chatId)
            val markup = Menu.entries.map { QueryData(it.label, it.callbackData) }
            Result.Send(textRepository.foodSurveyMenuPrompt(), markup)
        }
    }

    private fun handleMenuSelect(chatId: Long, menu: Menu): Result {
        val currentInfo = userStates.getOrDefault(chatId, FoodInfo(menuChoice = menu))
        userStates[chatId] = currentInfo.copy(menuChoice = menu)
        return Result.Edit(textRepository.foodSurveyDrinkPrompt(), createDrinkMarkup(setOf()))
    }

    private fun handleDrinkSelect(chatId: Long, drink: Drink): Result {
        val currentInfo = userStates[chatId] ?: return Result.Error(textRepository.internalError())
        val newDrinks = if (currentInfo.drinks.contains(drink)) {
            currentInfo.drinks - drink
        } else {
            currentInfo.drinks + drink
        }
        userStates[chatId] = currentInfo.copy(drinks = newDrinks)
        return Result.Edit(textRepository.foodSurveyDrinkPrompt(), createDrinkMarkup(newDrinks))
    }

    private fun handleDrinkFinish(chatId: Long): Result {
        val currentInfo = userStates[chatId] ?: return Result.Error(textRepository.internalError())
        return if (currentInfo.drinks.isEmpty()) {
            Result.Edit(textRepository.foodSurveyDrinkPromptAtLeastSingle(), createDrinkMarkup(setOf()))
        } else {
            usersEnteringAdditional.add(chatId)
            Result.Edit(textRepository.foodSurveyAdditionalPrompt(), listOf(
                QueryData(textRepository.foodSurveySkipAdditional(), callbackSkipAdditional)
            ))
        }
    }

    private fun handleAdditionalInfo(chatId: Long, text: String): Result {
        val user = userRepository.getByChatId(chatId).getOrElse {
            return Result.Error(textRepository.userNotFound())
        }
        val currentInfo = userStates.remove(chatId) ?: return Result.Error(textRepository.internalError())
        val finalFoodInfo = currentInfo.copy(additional = text)
        userRepository.update(user.copy(foodInfo = finalFoodInfo))
        usersEnteringAdditional.remove(chatId)
        return Result.Finished(textRepository.foodSurveyFinished())
    }

    private fun createDrinkMarkup(selected: Set<Drink>): List<QueryData> {
        val markup = Drink.entries.map { drink ->
            val label = if (selected.contains(drink)) "✅ ${drink.label}" else drink.label
            QueryData(label, drink.callbackData)
        }.toMutableList()
        markup.add(QueryData(textRepository.foodSurveyDrinkDoneButton(), callbackDrinkFinish))
        return markup
    }

    private val Menu.label: String
        get() = when (this) {
            Menu.HROOHROO -> textRepository.foodSurveyMenuHroohroo()
            Menu.REEBOK -> textRepository.foodSurveyMenuReebok()
            Menu.VEGI -> textRepository.foodSurveyMenuVegi()
        }

    private val Menu.callbackData: String
        get() = callbackPrefixFood + this.name.lowercase()

    private val String.menu: Menu?
        get() = Menu.entries.find { it.callbackData == this }

    private val Drink.label: String
        get() = when (this) {
            Drink.WHITE -> textRepository.foodSurveyDrinkWhite()
            Drink.RED -> textRepository.foodSurveyDrinkRed()
            Drink.SHAMPOO -> textRepository.foodSurveyDrinkShampoo()
            Drink.WHISKEY -> textRepository.foodSurveyDrinkWhiskey()
            Drink.VODKA -> textRepository.foodSurveyDrinkVodka()
            Drink.ALCOHOLESS -> textRepository.foodSurveyDrinkAlcoholess()
        }

    private val Drink.callbackData: String
        get() = callbackPrefixDrink + this.name.lowercase()

    private val String.drink: Drink?
        get() = Drink.entries.find { it.callbackData == this }
}
