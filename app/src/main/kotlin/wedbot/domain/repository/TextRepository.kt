package wedbot.domain.repository

interface TextRepository {
    fun internalError(): String
    fun generateGreeting(name: String?): String
    fun infoMessage(): String
    fun helpMessage(): String
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
    fun menuUpdated(): String
    fun calendarMessage(): String
    fun adminPingStarted(): String
    fun adminPingPrompt(): String
    fun adminPingCancel(): String
    fun adminPingCancelButton(): String
    fun adminPingSucceed(): String
    fun adminMessageHeader(): String
    fun menuButtonInfo(): String
    fun menuButtonIcs(): String
    fun menuButtonLocation(): String
    fun menuButtonEventStatus(): String
    fun menuButtonHelp(): String
    fun menuButtonStatusTable(): String
    fun menuButtonPingGuests(): String
}