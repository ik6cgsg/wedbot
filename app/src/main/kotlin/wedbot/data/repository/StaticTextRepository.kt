package wedbot.data.repository

import wedbot.BotConstants
import wedbot.data.resources.BotMessages
import wedbot.domain.repository.TextRepository
import kotlin.text.format

class StaticTextRepository : TextRepository {
    override fun internalError(): String = BotMessages.INTERNAL_ERROR
    override fun totalSleeve(): String = BotMessages.TOTAL_SLEEVE
    // start & contact commands
    override fun generateGreeting(name: String?): String = BotMessages.GREETING
        .format(name?.let {", $it"} ?: "")
        .trimIndent()
        .plus("\n\n")
        .plus(infoMessage())
    override fun infoMessage(): String = BotMessages.INFO_MESSAGE
        //.format(BotConstants.eventStatusDeadline)
        .trimIndent()
    override fun helpMessage(): String = BotMessages.HELP_MESSAGE
        .escapeMarkdown()
        .trimIndent()
    override fun shareContactError(): String = BotMessages.ASK_PHONE
    override fun shareContactLabel(): String = BotMessages.ASK_PHONE_LABEL
    override fun userNotFound(): String = BotMessages.USER_NOT_FOUND
    override fun weakRights(): String = BotMessages.WEAK_RIGHTS
    override fun alreadyRegistered(): String = BotMessages.USER_ALREADY_REGISTERED
    // Event status
    override fun eventStatusPingFirst(): String = BotMessages.EVENT_STATUS_PING_FIRST
    override fun eventStatusPingDaily(name: String?): String = BotMessages.EVENT_STATUS_PING_DAILY
        .format(name ?: "гость")
        .trimIndent()
    override fun eventStatusAcceptButton(): String = BotMessages.EVENT_STATUS_ACCEPT_BUTTON
    override fun eventStatusRejectButton(): String = BotMessages.EVENT_STATUS_REJECT_BUTTON
    override fun eventStatusThinkButton(): String = BotMessages.EVENT_STATUS_THINK_BUTTON
    override fun eventStatusAccepted(): String = BotMessages.EVENT_STATUS_ACCEPTED
    override fun eventStatusRejected(): String = BotMessages.EVENT_STATUS_REJECTED
    override fun eventStatusThinkAgain(): String = BotMessages.EVENT_STATUS_THINK_AGAIN
        .format(BotConstants.eventStatusDeadline)
        .trimIndent()
    override fun statusThinkAgain(deadline: String): String = BotMessages.STATUS_THINK_AGAIN
        .format(deadline)
        .trimIndent()
    override fun villaStatusPingApproved(): String = BotMessages.VILLA_STATUS_PING_APPROVED
        .trimIndent()
    override fun villaStatusPingRejected(): String = BotMessages.VILLA_STATUS_PING_REJECTED
        .trimIndent()
    override fun villaStatusPingThinking(deadline: String): String = BotMessages.VILLA_STATUS_PING_THINKING
        .format(deadline)
        .trimIndent()
    override fun villaStatusAcceptButton(): String = BotMessages.VILLA_STATUS_ACCEPT_BUTTON
    override fun villaStatusRejectButton(): String = BotMessages.VILLA_STATUS_REJECT_BUTTON
    override fun villaStatusThinkButton(): String = BotMessages.VILLA_STATUS_THINK_BUTTON
    override fun villaStatusAccepted(): String = BotMessages.VILLA_STATUS_ACCEPTED
        .trimIndent()
    override fun villaStatusRejected(): String = BotMessages.VILLA_STATUS_REJECTED
        .trimIndent()
    override fun menuMessage(): String = BotMessages.MENU_MESSAGE
        .trimIndent()
    override fun menuUpdated(): String = BotMessages.MENU_UPDATED
    override fun calendarMessage(): String = BotMessages.CALENDAR_MESSAGE
        .trimIndent()
    override fun adminPingStarted(): String = BotMessages.ADMIN_PING_STARTED
    override fun adminPingAllPrompt(): String = BotMessages.ADMIN_PING_ALL_PROMPT
        .trimIndent()
    override fun adminPingAllButton(): String = BotMessages.ADMIN_PING_ALL_BUTTON
    override fun adminPingGuestsPrompt(): String = BotMessages.ADMIN_PING_GUESTS_PROMPT
        .trimIndent()
    override fun adminPingGuestsButton(): String = BotMessages.ADMIN_PING_GUESTS_BUTTON
    override fun adminPingCancel(): String = BotMessages.ADMIN_PING_CANCEL
    override fun adminPingCancelButton(): String = BotMessages.ADMIN_PING_CANCEL_BUTTON
    override fun adminPingSucceed(): String = BotMessages.ADMIN_PING_SUCCEED
    override fun adminMessageHeader(): String = BotMessages.ADMIN_MSG_HEADER
    override fun menuButtonInfo(): String = BotMessages.MENU_BUTTON_INFO
    override fun menuButtonIcs(): String = BotMessages.MENU_BUTTON_ICS
    override fun menuButtonLocation(): String = BotMessages.MENU_BUTTON_LOCATION
    override fun menuButtonEventStatus(): String = BotMessages.MENU_BUTTON_EVENT_STATUS
    override fun menuButtonHelp(): String = BotMessages.MENU_BUTTON_HELP
    override fun menuButtonStatusTable(): String = BotMessages.MENU_BUTTON_STATUS_TABLE
    override fun menuButtonPingGuests(): String = BotMessages.MENU_BUTTON_PING_GUESTS
    override fun menuButtonDressCode(): String = BotMessages.MENU_BUTTON_DRESS_CODE
    override fun menuButtonVillaStatus(): String = BotMessages.MENU_BUTTON_VILLA_STATUS
    override fun menuHasSurveys(): String = BotMessages.MENU_HAS_SURVEYS
    override fun techWorks(): String = BotMessages.TECH_WORKS

    private fun String.escapeMarkdown(): String = this
        .replace("_", "\\_")
        .replace("*", "\\*")
        .replace("[", "\\[")
        .replace("`", "\\`")
}
