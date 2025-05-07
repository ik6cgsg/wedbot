package wedbot

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.http.HttpStatusCode
import com.github.kotlintelegrambot.Bot

class Server(
    val bot: WedBot
) {
    fun start() {
        embeddedServer(
            factory = Netty,
            configure = { configuration() },
            module = { module() }
        ).start(wait = true)
    }

    private fun ApplicationEngine.Configuration.configuration() {
        sslConnector(
            keyStore = CertificateUtils.keyStore,
            keyAlias = CertificateUtils.keyAlias,
            keyStorePassword = { CertificateUtils.keyStorePassword },
            privateKeyPassword = { CertificateUtils.keyStorePassword }
        ) {
            port = 8443
            host = SystemProperties.botHost
            keyStorePath = CertificateUtils.keyStoreFile.absoluteFile
        }
    }

    private fun Application.module() {
        routing {
            post("/${SystemProperties.botToken}") {
                val response = call.receiveText()
                bot.process(response)
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
