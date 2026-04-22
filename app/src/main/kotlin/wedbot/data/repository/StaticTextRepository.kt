package wedbot.data.repository

import wedbot.BotConstants
import wedbot.data.resources.BotMessages
import wedbot.domain.repository.TextRepository

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
    override fun shareOwnContactOnlyError(): String = BotMessages.ASK_PHONE_OWN_CONTACT_ONLY
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
    override fun menuButtonTransfer(): String = BotMessages.MENU_BUTTON_TRANSFER
    override fun menuButtonFood(): String = BotMessages.MENU_BUTTON_FOOD
    override fun menuHasSurveys(): String = BotMessages.MENU_HAS_SURVEYS
        .trimIndent()
    override fun menuSurveysDoubleClick(): String = BotMessages.MENU_SURVEYS_DOUBLE_CLICK
        .trimIndent()

    override fun techWorks(): String = BotMessages.TECH_WORKS

    // Transfer status implementations
    override fun transferStatusThinkingButton(): String = BotMessages.TRANSFER_STATUS_THINKING_BUTTON
    override fun transferStatusNeedButton(): String = BotMessages.TRANSFER_STATUS_NEED_BUTTON
    override fun transferStatusSelfHandleButton(): String = BotMessages.TRANSFER_STATUS_SELF_HANDLE_BUTTON
    override fun transferStatusSocialLegendButton(): String = BotMessages.TRANSFER_STATUS_SOCIAL_LEGEND_BUTTON
    override fun transferStatusPingThinking(deadline: String): String = BotMessages.TRANSFER_STATUS_PING_THINKING
        .format(deadline)
        .trimIndent()
    override fun transferStatusPingNeed(): String = BotMessages.TRANSFER_STATUS_PING_NEED
    override fun transferStatusPingSelfHandle(): String = BotMessages.TRANSFER_STATUS_PING_SELF_HANDLE
    override fun transferStatusPingSocialLegend(): String = BotMessages.TRANSFER_STATUS_PING_SOCIAL_LEGEND
    override fun transferStatusNeedChoice(): String = BotMessages.TRANSFER_STATUS_NEED_CHOICE
    override fun transferStatusSelfHandleChoice(): String = BotMessages.TRANSFER_STATUS_SELF_HANDLE_CHOICE
    override fun transferStatusSocialLegendChoice(): String = BotMessages.TRANSFER_STATUS_SOCIAL_LEGEND_CHOICE

    // Food & Drink survey
    override fun foodSurveyMenuPrompt(): String = BotMessages.FOOD_SURVEY_MENU_PROMPT
    override fun foodSurveyDrinkPrompt(): String = BotMessages.FOOD_SURVEY_DRINK_PROMPT
    override fun foodSurveyDrinkPromptAtLeastSingle(): String = BotMessages.FOOD_SURVEY_DRINK_PROMPT_AT_LEAST_SINGLE
    override fun foodSurveyAdditionalPrompt(): String = BotMessages.FOOD_SURVEY_ADDITIONAL_PROMPT
    override fun foodSurveyMenuHroohroo(): String = BotMessages.FOOD_SURVEY_MENU_HROOHROO
    override fun foodSurveyMenuReebok(): String = BotMessages.FOOD_SURVEY_MENU_REEBOK
    override fun foodSurveyMenuVegi(): String = BotMessages.FOOD_SURVEY_MENU_VEGI
    override fun foodSurveyDrinkWhite(): String = BotMessages.FOOD_SURVEY_DRINK_WHITE
    override fun foodSurveyDrinkRed(): String = BotMessages.FOOD_SURVEY_DRINK_RED
    override fun foodSurveyDrinkShampoo(): String = BotMessages.FOOD_SURVEY_DRINK_SHAMPOO
    override fun foodSurveyDrinkWhiskey(): String = BotMessages.FOOD_SURVEY_DRINK_WHISKEY
    override fun foodSurveyDrinkVodka(): String = BotMessages.FOOD_SURVEY_DRINK_VODKA
    override fun foodSurveyDrinkAlcoholess(): String = BotMessages.FOOD_SURVEY_DRINK_ALCOHOLESS
    override fun foodSurveyDrinkDoneButton(): String = BotMessages.FOOD_SURVEY_DRINK_DONE_BUTTON
    override fun foodSurveySkipAdditional(): String = BotMessages.FOOD_SURVEY_SKIP_ADDITIONAL
    override fun foodSurveyFinished(): String = BotMessages.FOOD_SURVEY_FINISHED
    override fun foodSurveySummary(menu: String, drinks: String, additional: String): String = 
        BotMessages.FOOD_SURVEY_SUMMARY.format(menu, drinks, additional).trimIndent()

    private fun String.escapeMarkdown(): String = this
        .replace("_", "\\_")
        .replace("*", "\\*")
        .replace("[", "\\[")
        .replace("`", "\\`")
}
