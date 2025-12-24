package wedbot

import wedbot.data.db.DatabaseSqlite
import wedbot.data.repository.ExposedUserRepository
import wedbot.data.repository.StaticTextRepository
import wedbot.domain.usecase.StartUseCase
import wedbot.domain.usecase.VerifyPhoneUseCase
import wedbot.presentation.WedBot

fun main() {
    val db = DatabaseSqlite()
    val userRepository = ExposedUserRepository(db)
    val textRepository = StaticTextRepository()
    val startUseCase = StartUseCase(userRepository, textRepository)
    val verifyPhoneUseCase = VerifyPhoneUseCase(userRepository, textRepository)
    val wedbot = WedBot(
        startUseCase,
        verifyPhoneUseCase
    )
    wedbot.start()
    if (SystemProperties.useWebhook) {
        val server = Server(wedbot)
        server.start()
    }
}
