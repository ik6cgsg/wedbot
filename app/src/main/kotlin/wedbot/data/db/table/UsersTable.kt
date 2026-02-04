package wedbot.data.db.table

import org.jetbrains.exposed.sql.Table
import wedbot.domain.entity.FoodInfo
import wedbot.domain.entity.Role
import wedbot.domain.entity.Sex
import wedbot.domain.entity.Status
import wedbot.domain.entity.TransferStatus

object UsersTable: Table("users") {
    val id = integer("id").autoIncrement()
    val chatId = long("chat_id").uniqueIndex().nullable()
    val username = varchar("username", 255).uniqueIndex().nullable()
    val name = varchar("name", 255).nullable()
    val phone = varchar("phone", 30).uniqueIndex().nullable()
    val alias = varchar("alias", 255).nullable()
    val sex = enumerationByName("sex", 30, Sex::class).default(Sex.NE_BYLO)
    val eventStatus = enumerationByName("event_status", 30, Status::class).default(Status.THINKING)
    val villaStatus = enumerationByName("villa_status", 30, Status::class).default(Status.THINKING)
    val transferStatus = enumerationByName("transfer_status", 30, TransferStatus::class).default(TransferStatus.THINKING)
    val role = enumerationByName("role", 30, Role::class).default(Role.GUEST)
    val foodInfoId = integer("food_info_id").references(FoodInfoTable.id).nullable()

    override val primaryKey = PrimaryKey(id)
}