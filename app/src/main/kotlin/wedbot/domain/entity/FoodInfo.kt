package wedbot.domain.entity

enum class Menu {
    TAVUK, HROOHROO, VEGI
}

enum class Drink {
    WHITE, RED, SHAMPOO, WHISKEY, VODKA, ALCOHOLESS
}

data class FoodInfo(
    val id: Int = 0,
    val additional: String = "",
    val menuChoice: Menu = Menu.TAVUK,
    val drinks: Set<Drink> = setOf()
)