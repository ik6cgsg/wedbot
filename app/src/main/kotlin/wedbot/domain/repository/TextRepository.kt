package wedbot.domain.repository

interface TextRepository {
    fun internalError(): String
    fun totalSleeve(): String
    fun generateGreeting(name: String?): String
    fun infoMessage(): String
    fun helpMessage(): String
    fun shareContactError(): String
    fun shareContactLabel(): String
    fun shareOwnContactOnlyError(): String
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
    fun statusThinkAgain(deadline: String): String
    fun villaStatusPingApproved(): String
    fun villaStatusPingRejected(): String
    fun villaStatusPingThinking(deadline: String): String
    fun villaStatusAcceptButton(): String
    fun villaStatusRejectButton(): String
    fun villaStatusThinkButton(): String
    fun villaStatusAccepted(): String
    fun villaStatusRejected(): String
    fun menuMessage(): String
    fun menuUpdated(): String
    fun calendarMessage(): String
    fun adminPingStarted(): String
    fun adminPingAllPrompt(): String
    fun adminPingAllButton(): String
    fun adminPingGuestsPrompt(): String
    fun adminPingGuestsButton(): String
    fun adminPingCancel(): String
    fun adminPingCancelButton(): String
    fun adminPingSucceed(): String
    fun adminMessageHeader(): String
    // Menu buttons
    fun menuButtonInfo(): String
    fun menuButtonIcs(): String
    fun menuButtonLocation(): String
    fun menuButtonEventStatus(): String
    fun menuButtonHelp(): String
    fun menuButtonStatusTable(): String
    fun menuButtonPingGuests(): String
    fun menuButtonDressCode(): String
    fun menuButtonVillaStatus(): String
    fun menuButtonTransfer(): String
    fun menuButtonFood(): String
    fun menuHasSurveys(): String
    fun menuSurveysDoubleClick(): String
    fun techWorks(): String
    // Transfer
    fun transferStatusThinkingButton(): String
    fun transferStatusNeedButton(): String
    fun transferStatusSelfHandleButton(): String
    fun transferStatusSocialLegendButton(): String
    fun transferStatusPingThinking(deadline: String): String
    fun transferStatusPingNeed(): String
    fun transferStatusPingSelfHandle(): String
    fun transferStatusPingSocialLegend(): String
    fun transferStatusNeedChoice(): String
    fun transferStatusSelfHandleChoice(): String
    fun transferStatusSocialLegendChoice(): String
    // Food & Drink survey
    fun foodSurveyMenuPrompt(): String
    fun foodSurveyDrinkPrompt(): String
    fun foodSurveyDrinkPromptAtLeastSingle(): String
    fun foodSurveyAdditionalPrompt(): String
    fun foodSurveyMenuHroohroo(): String
    fun foodSurveyMenuReebok(): String
    fun foodSurveyMenuVegi(): String
    fun foodSurveyDrinkWhite(): String
    fun foodSurveyDrinkRed(): String
    fun foodSurveyDrinkShampoo(): String
    fun foodSurveyDrinkWhiskey(): String
    fun foodSurveyDrinkVodka(): String
    fun foodSurveyDrinkAlcoholess(): String
    fun foodSurveyDrinkDoneButton(): String
    fun foodSurveySkipAdditional(): String
    fun foodSurveyFinished(): String
    fun foodSurveySummary(menu: String, drinks: String, additional: String): String
}
