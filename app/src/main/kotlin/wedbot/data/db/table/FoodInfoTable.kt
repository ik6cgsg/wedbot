package wedbot.data.db.table

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import wedbot.domain.entity.Drink
import wedbot.domain.entity.Menu

object FoodInfoTable: Table("food_infos") {
    val id = integer("id").autoIncrement()
    val additional = text("additional")
    val menu = enumerationByName("menu", 30, Menu::class).default(Menu.TAVUK)
    override val primaryKey = PrimaryKey(id)
}

object FoodInfoDrinksTable: Table("food_info_drinks") {
    val foodInfoId = integer("food_info_id").references(FoodInfoTable.id, onDelete = ReferenceOption.CASCADE)
    val drink = enumerationByName("drink", 30, Drink::class)
    override val primaryKey = PrimaryKey(foodInfoId, drink)
}