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
import wedbot.domain.usecase.HandleVillaStatusUseCase
import wedbot.domain.usecase.HandleTransferStatusUseCase
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
import wedbot.presentation.dispatcher.VillaStatusDispatcher
import wedbot.presentation.dispatcher.TransferStatusDispatcher
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
    val handleVillaStatusUseCase = HandleVillaStatusUseCase(userRepository, textRepository)
    val handleTransferStatusUseCase = HandleTransferStatusUseCase(userRepository, textRepository)
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
    val villaStatusDispatcher: VillaStatusDispatcher
    val transferStatusDispatcher: TransferStatusDispatcher
    val menuDispatcher: MenuDispatcher
    val statusTableDispatcher: StatusTableDispatcher
    val pingDispatcher: PingDispatcher
    val easterDispatcher: EasterDispatcher
    val dummyDispatcher: DummyDispatcher
    // Bot
    val wedbot: WedBot

    init {
        // Dispatchers
        eventStatusDispatcher = EventStatusDispatcher(textRepository, handleEventStatusUseCase)
        authDispatcher = AuthDispatcher(textRepository, startUseCase, verifyPhoneUseCase) { bot, id ->
            eventStatusDispatcher.pingEventStatusFirst(bot, id)
        }
        villaStatusDispatcher = VillaStatusDispatcher(textRepository, handleVillaStatusUseCase)
        transferStatusDispatcher = TransferStatusDispatcher(textRepository, handleTransferStatusUseCase)
        statusTableDispatcher = StatusTableDispatcher(statusTableUseCase)
        pingDispatcher = PingDispatcher(textRepository, pingGuestsUseCase)
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

                override fun pingVillaStatus(bot: Bot, chatId: Long) {
                    villaStatusDispatcher.pingVillaStatus(bot, chatId)
                }

                override fun pingTransferStatus(bot: Bot, chatId: Long) {
                    transferStatusDispatcher.pingTransferStatus(bot, chatId)
                }
            }
        )
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
            dummyDispatcher,
            villaStatusDispatcher
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

    override fun hasIdleSurveys(chatId: Long) {
        wedbot.pingSurveys(chatId)
    }
}
