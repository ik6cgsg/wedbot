package wedbot.data.db

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import wedbot.SystemProperties
import wedbot.data.db.table.FoodInfoDrinksTable
import wedbot.data.db.table.FoodInfoTable
import wedbot.data.db.table.UsersTable
import wedbot.domain.entity.Drink
import wedbot.domain.entity.FoodInfo
import wedbot.domain.entity.Menu
import wedbot.domain.entity.Role
import wedbot.domain.entity.Status
import wedbot.domain.entity.UserInfo
import wedbot.domain.entity.UserStatus

class DatabaseSqlite {
    private val dbPath = "jdbc:sqlite:res/wed.db"
    private val driver = "org.sqlite.JDBC"

    init {
        Database.connect(dbPath, driver)
        transaction {
            SchemaUtils.create(UsersTable)
            SchemaUtils.create(FoodInfoTable)
            SchemaUtils.create(FoodInfoDrinksTable)
        }
        if (SystemProperties.dbNeedInit) {
            transaction {
                UsersTable.deleteAll()
                FoodInfoTable.deleteAll()
                FoodInfoDrinksTable.deleteAll()
            }
            create(UserInfo(phone = "79119889011", username = "cgsgilich"))
            create(UserInfo(username = "dergoleem"))
            create(UserInfo(
                username = "fakecgsgilich",
                foodInfo = FoodInfo(
                    additional = "big cock",
                    menuChoice = Menu.HROOHROO,
                    drinks = setOf(Drink.WHISKEY, Drink.RED, Drink.WHISKEY)
                )
            ))
        }
    }

    fun getUserByUsername(username: String): UserInfo? = transaction {
        UsersTable.leftJoin(FoodInfoTable)
            .selectAll()
            .where { UsersTable.username eq username }
            .singleOrNull()
            ?.toUserInfo()
    }

    fun getUserByPhone(phone: String): UserInfo? = transaction {
        UsersTable.leftJoin(FoodInfoTable)
            .selectAll()
            .where { UsersTable.phone eq phone }
            .singleOrNull()
            ?.toUserInfo()
    }

    fun getUserByChatId(chatId: Long): UserInfo? = transaction {
        UsersTable.leftJoin(FoodInfoTable)
            .selectAll()
            .where { UsersTable.chatId eq chatId }
            .singleOrNull()
            ?.toUserInfo()
    }

    fun getUserStatuses(offset: Long?, limit: Int?): List<UserStatus> = transaction {
        UsersTable
            .select(UsersTable.chatId, UsersTable.username, UsersTable.name,
                UsersTable.eventStatus, UsersTable.villaStatus, UsersTable.transferStatus, UsersTable.foodInfoId
            )
            .where { UsersTable.chatId neq null }
            .offset(offset ?: 0)
            .limit(limit ?: Int.MAX_VALUE)
            .map { it.toUserStatus() }
    }

    fun getAllUserChatIds(): List<Long> = transaction {
        UsersTable
            .select(UsersTable.chatId)
            .where { UsersTable.chatId neq null }
            .map { it[UsersTable.chatId]!! }
    }

    fun getGuestsChatIds(): List<Long> = transaction {
        UsersTable
            .select(UsersTable.chatId)
            .where { (UsersTable.chatId neq null) and (UsersTable.eventStatus eq Status.APPROVED) }
            .map { it[UsersTable.chatId]!! }
    }

    fun getAdminChatIds(): List<Long> = transaction {
        UsersTable
            .select(UsersTable.chatId)
            .where { (UsersTable.role eq Role.ADMIN) and (UsersTable.chatId neq null) }
            .map { it[UsersTable.chatId]!! }
    }


    fun create(user: UserInfo) = transaction {
        val newFoodInfoId = if (user.foodInfo != null) {
            val id = FoodInfoTable.insert { foodInfoTable ->
                foodInfoTable[additional] = user.foodInfo.additional
                foodInfoTable[menu] = user.foodInfo.menuChoice
            } get FoodInfoTable.id
            user.foodInfo.drinks.forEach { drinkVal ->
                FoodInfoDrinksTable.insert { foodInfoDrinksTable ->
                    foodInfoDrinksTable[foodInfoId] = id
                    foodInfoDrinksTable[drink] = drinkVal
                }
            }
            id
        } else null
        UsersTable.insert {
            it[UsersTable.chatId] = user.chatId
            it[UsersTable.username] = user.username
            it[UsersTable.name] = user.name
            it[UsersTable.phone] = user.phone
            it[UsersTable.alias] = user.alias
            it[UsersTable.sex] = user.sex
            it[UsersTable.eventStatus] = user.eventStatus
            it[UsersTable.villaStatus] = user.villaStatus
            it[UsersTable.transferStatus] = user.transferStatus
            it[UsersTable.role] = user.role
            it[UsersTable.foodInfoId] = newFoodInfoId
        }
    }

    fun update(user: UserInfo) = transaction {
        var currentFoodInfoId = user.foodInfo?.id
        if (user.foodInfo != null) {
            if (user.foodInfo.id == 0) { // new food entry
                currentFoodInfoId = FoodInfoTable.insert {
                    it[additional] = user.foodInfo.additional
                    it[menu] = user.foodInfo.menuChoice
                } get FoodInfoTable.id
                user.foodInfo.drinks.forEach { drinkVal ->
                    FoodInfoDrinksTable.insert {
                        it[foodInfoId] = currentFoodInfoId
                        it[drink] = drinkVal
                    }
                }
            } else { // update old one
                FoodInfoTable.update({ FoodInfoTable.id eq user.foodInfo.id }) {
                    it[additional] = user.foodInfo.additional
                    it[menu] = user.foodInfo.menuChoice
                }
                // clear current relations
                FoodInfoDrinksTable.deleteWhere { foodInfoId eq user.foodInfo.id }
                user.foodInfo.drinks.forEach { drinkVal ->
                    FoodInfoDrinksTable.insert {
                        it[foodInfoId] = user.foodInfo.id
                        it[drink] = drinkVal
                    }
                }
            }
        }
        UsersTable.update({ UsersTable.id eq user.id }) {
            it[chatId] = user.chatId
            it[username] = user.username
            it[name] = user.name
            it[phone] = user.phone
            it[alias] = user.alias
            it[sex] = user.sex
            it[eventStatus] = user.eventStatus
            it[villaStatus] = user.villaStatus
            it[transferStatus] = user.transferStatus
            it[role] = user.role
            it[foodInfoId] = currentFoodInfoId
        }
    }

    private fun ResultRow.toUserInfo(): UserInfo {
        val foodInfoId = this[UsersTable.foodInfoId]
        val foodInfo = if (foodInfoId != null) {
            val drinks = FoodInfoDrinksTable
                .selectAll().where { FoodInfoDrinksTable.foodInfoId eq foodInfoId }
                .map { it[FoodInfoDrinksTable.drink] }
                .toSet()
            FoodInfo(
                id = foodInfoId,
                additional = this[FoodInfoTable.additional],
                menuChoice = this[FoodInfoTable.menu],
                drinks = drinks
            )
        } else {
            null
        }
        return UserInfo(
            this[UsersTable.id],
            this[UsersTable.chatId],
            this[UsersTable.username],
            this[UsersTable.name],
            this[UsersTable.phone],
            this[UsersTable.alias],
            this[UsersTable.sex],
            this[UsersTable.eventStatus],
            this[UsersTable.villaStatus],
            this[UsersTable.transferStatus],
            this[UsersTable.role],
            foodInfo
        )
    }

    private fun ResultRow.toUserStatus() = UserStatus(
        this[UsersTable.chatId]!!,
        this[UsersTable.username],
        this[UsersTable.name],
        this[UsersTable.eventStatus],
        this[UsersTable.villaStatus],
        this[UsersTable.transferStatus],
        this[UsersTable.foodInfoId] != null
    )
}
