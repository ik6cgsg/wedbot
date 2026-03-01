package wedbot.domain.entity

enum class Sex {
    MALE, FEMALE, NE_BYLO
}

enum class Status {
    APPROVED, SLEEVE, THINKING
}

enum class TransferStatus {
    THINKING, NEED, SELF_HANDLE, SOCIAL_LEGEND
}

enum class Role {
    ADMIN, STAFF, GUEST
}

data class UserInfo(
    val id: Int = 0,
    val chatId: Long? = null,
    val username: String? = null,
    val name: String? = null,
    val phone: String? = null,
    val alias: String? = null,
    val sex: Sex = Sex.NE_BYLO,
    val eventStatus: Status = Status.THINKING,
    val villaStatus: Status = Status.THINKING,
    val transferStatus: TransferStatus = TransferStatus.THINKING,
    val role: Role = Role.GUEST,
    val foodInfo: FoodInfo? = null
)

fun UserInfo.toUserStatus(): UserStatus? {
    return UserStatus(
        this.chatId ?: return null,
        this.username,
        this.name,
        this.eventStatus,
        this.villaStatus,
        this.transferStatus,
        this.foodInfo != null
    )
}

data class UserStatus(
    val chatId: Long,
    val username: String?,
    val name: String?,
    val eventStatus: Status,
    val villaStatus: Status,
    val transferStatus: TransferStatus,
    val foodDrinkChosen: Boolean
)
