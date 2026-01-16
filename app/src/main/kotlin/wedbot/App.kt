package wedbot

import com.github.kotlintelegrambot.Bot
import wedbot.data.db.DatabaseSqlite
import wedbot.data.repository.ExposedUserRepository
import wedbot.data.repository.StaticTextRepository
import wedbot.domain.service.NotificationListener
import wedbot.domain.service.NotificationService
import wedbot.domain.usecase.CalendarUseCase
import wedbot.domain.usecase.DressCodeUseCase
import wedbot.domain.usecase.EasterUseCase
import wedbot.domain.usecase.HandleEventStatusUseCase
import wedbot.domain.usecase.InfoUseCase
import wedbot.domain.usecase.LocationUseCase
import wedbot.domain.usecase.MenuUseCase
import wedbot.domain.usecase.PingGuestsUseCase
import wedbot.domain.usecase.StartUseCase
import wedbot.domain.usecase.StatusTableUseCase
import wedbot.domain.usecase.VerifyPhoneUseCase
import wedbot.presentation.WedBot
import wedbot.presentation.dispatcher.AuthDispatcher
import wedbot.presentation.dispatcher.DummyDispatcher
import wedbot.presentation.dispatcher.EasterDispatcher
import wedbot.presentation.dispatcher.EventStatusDispatcher
import wedbot.presentation.dispatcher.MenuDispatcher
import wedbot.presentation.dispatcher.MenuEventInterface
import wedbot.presentation.dispatcher.PingDispatcher
import wedbot.presentation.dispatcher.StatusTableDispatcher
import wedbot.presentation.server.Server

fun main() {
    val app = Application()
    app.start()
}

class Application: NotificationListener {
    val db = DatabaseSqlite()
    // Reps
    val userRepository = ExposedUserRepository(db)
    val textRepository = StaticTextRepository()
    // UseCases
    val startUseCase = StartUseCase(userRepository, textRepository)
    val verifyPhoneUseCase = VerifyPhoneUseCase(userRepository, textRepository)
    val handleEventStatusUseCase = HandleEventStatusUseCase(userRepository, textRepository)
    val menuUseCase = MenuUseCase(userRepository, textRepository)
    val calendarUseCase = CalendarUseCase(userRepository, textRepository)
    val locationUseCase = LocationUseCase(userRepository, textRepository)
    val statusTableUseCase = StatusTableUseCase(userRepository, textRepository)
    val infoUseCase = InfoUseCase(userRepository, textRepository)
    val dressCodeUseCase = DressCodeUseCase(userRepository, textRepository)
    val pingGuestsUseCase = PingGuestsUseCase(userRepository, textRepository)
    val easterUseCase = EasterUseCase()
    // Services
    val notificationService = NotificationService(userRepository, this)
    // Dispatchers
    val authDispatcher: AuthDispatcher
    val eventStatusDispatcher: EventStatusDispatcher
    val menuDispatcher: MenuDispatcher
    val statusTableDispatcher: StatusTableDispatcher
    val pingDispatcher: PingDispatcher
    val easterDispatcher: EasterDispatcher
    val dummyDispatcher: DummyDispatcher
    // Bot
    val wedbot: WedBot

    init {
        // Dispatchers
        authDispatcher = AuthDispatcher(textRepository, startUseCase, verifyPhoneUseCase) { bot, id ->
            eventStatusDispatcher.pingEventStatusFirst(bot, id)
        }
        eventStatusDispatcher = EventStatusDispatcher(textRepository, handleEventStatusUseCase)
        menuDispatcher = MenuDispatcher(
            textRepository, menuUseCase, infoUseCase, dressCodeUseCase, calendarUseCase, locationUseCase,
            object : MenuEventInterface {
                override fun needToHandle(bot: Bot, chatId: Long): Boolean {
                    return !pingDispatcher.isUserInPingMode(chatId)
                }

                override fun pingEventStatusFirst(bot: Bot, chatId: Long) {
                    eventStatusDispatcher.pingEventStatusFirst(bot, chatId)
                }

                override fun showStatusTable(bot: Bot, chatId: Long) {
                    statusTableDispatcher.showStatusTable(bot, chatId)
                }

                override fun startPingGuestsFlow(bot: Bot, chatId: Long) {
                    pingDispatcher.startPingGuestsFlow(bot, chatId)
                }
            }
        )
        statusTableDispatcher = StatusTableDispatcher(statusTableUseCase)
        pingDispatcher = PingDispatcher(textRepository, pingGuestsUseCase)
        easterDispatcher = EasterDispatcher(easterUseCase)
        dummyDispatcher = DummyDispatcher(textRepository)
        // Super Mega WedBot initialization
        wedbot = WedBot(
            authDispatcher,
            eventStatusDispatcher,
            menuDispatcher,
            statusTableDispatcher,
            pingDispatcher,
            easterDispatcher,
            dummyDispatcher
        )
    }

    fun start() {
        wedbot.start()
        notificationService.startDailyReminder()
        if (SystemProperties.useWebhook) {
            val server = Server {
                wedbot.process(it)
            }
            server.start()
        }
    }

    override fun stillThinkingAboutEvent(chatId: Long, name: String?) {
        wedbot.pingEventStatus(chatId, name)
    }
}
