package wedbot.domain.entity

enum class Menu {
    HROOHROO, REEBOK, VEGI
}

enum class Drink {
    WHITE, RED, SHAMPOO, WHISKEY, VODKA, ALCOHOLESS
}

data class FoodInfo(
    val id: Int = 0,
    val additional: String = "",
    val menuChoice: Menu = Menu.HROOHROO,
    val drinks: Set<Drink> = setOf()
)