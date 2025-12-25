package wedbot

import wedbot.data.db.DatabaseSqlite
import wedbot.data.repository.ExposedUserRepository
import wedbot.data.repository.StaticTextRepository
import wedbot.domain.service.NotificationListener
import wedbot.domain.service.NotificationService
import wedbot.domain.usecase.CalendarUseCase
import wedbot.domain.usecase.HandleEventStatusUseCase
import wedbot.domain.usecase.LocationUseCase
import wedbot.domain.usecase.MenuUseCase
import wedbot.domain.usecase.StartUseCase
import wedbot.domain.usecase.VerifyPhoneUseCase
import wedbot.presentation.WedBot
import wedbot.presentation.server.Server

fun main() {
    val app = Application()
    app.start()
}

class Application: NotificationListener {
    val db = DatabaseSqlite()
    val userRepository = ExposedUserRepository(db)
    val textRepository = StaticTextRepository()
    val startUseCase = StartUseCase(userRepository, textRepository)
    val verifyPhoneUseCase = VerifyPhoneUseCase(userRepository, textRepository)
    val handleEventStatusUseCase = HandleEventStatusUseCase(userRepository, textRepository)
    val menuUseCase = MenuUseCase(userRepository, textRepository)
    val calendarUseCase = CalendarUseCase(userRepository, textRepository)
    val locationUseCase = LocationUseCase(userRepository, textRepository)
    val wedbot = WedBot(
        textRepository,
        startUseCase,
        verifyPhoneUseCase,
        handleEventStatusUseCase,
        menuUseCase,
        calendarUseCase,
        locationUseCase
    )
    val notificationService = NotificationService(userRepository, this)

    fun start() {
        wedbot.start()
        if (SystemProperties.useWebhook) {
            val server = Server {
                wedbot.process(it)
            }
            server.start()
        }
        notificationService.startDailyReminder()
    }

    override fun stillThinkingAboutEvent(chatId: Long, name: String?) {
        wedbot.pingEventStatus(chatId, name)
    }
}
