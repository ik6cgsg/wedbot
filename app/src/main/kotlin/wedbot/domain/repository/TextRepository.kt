package wedbot.domain.repository

interface TextRepository {
    fun internalError(): String
    fun generateGreeting(name: String?): String
    fun shareContactError(): String
    fun shareContactLabel(): String
    fun userNotFound(): String
    fun weakRights(): String
    fun alreadyRegistered(): String
    fun eventStatusPingFirst(): String
    fun eventStatusPingDaily(name: String?): String
    fun eventStatusAcceptButton(): String
    fun eventStatusRejectButton(): String
    fun eventStatusThinkButton(): String
    fun eventStatusAccepted(): String
    fun eventStatusRejected(): String
    fun eventStatusThinkAgain(): String
    fun menuMessage(): String
    fun calendarMessage(): String
}