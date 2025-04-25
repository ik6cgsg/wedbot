package wedbot

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.kotlin.datetime.*

enum class Status { ACCEPT, REJECT, NOT_SURE }
enum class Sex { MALE, FEMALE, NE_BYLO }

object Users: Table() {
    val id = integer("id").autoIncrement()
    val chatId = long("chat_id").uniqueIndex().nullable()
    val username = varchar("username", 255).nullable()
    val phone = varchar("phone", 18).nullable()
    val realName = varchar("real_name", 255)
    val nikName = varchar("nik_name", 255).nullable()
    val sex = enumerationByName("sex", 8, Sex::class).default(Sex.NE_BYLO)
    val status = enumerationByName("status", 8, Status::class).nullable()

    override val primaryKey = PrimaryKey(id)
}

class DBUtils {
    val dbPath = "jdbc:sqlite:res/data.db"
    val driver = "org.sqlite.JDBC"

    init {
        Database.connect(dbPath, driver)
        transaction {
            SchemaUtils.create(Users)
        }
        // TODO: if first run, init all chats and users
        transaction {
            Users.insert {
                it[username] = "cgsgilich"
                it[phone] = "+79990000000"
                it[realName] = "real_name"
                it[nikName] = "nik_name"
                it[status] = Status.ACCEPT
            }
            Users.insert {
                it[phone] = "+70000000"
                it[realName] = "real_name2"
            }
        }
    }

    fun getUserByUsername(username: String): ResultRow? {
        return transaction {
            Users.select(Users.id, Users.realName, Users.nikName, Users.sex, Users.status)
                .where { Users.username eq username }
                .singleOrNull()
        }
    }

    fun updateChatId(userId: Int, chatId: Long) {
        transaction {
            Users.upsert {
                it[id] = userId
                it[Users.chatId] = chatId
            }
        }
    }
}
