package wedbot.domain.repository

interface TextRepository {
    fun generateGreeting(name: String?): String
    fun shareContactError(): String
    fun shareContactLabel(): String
    fun notFound(): String
    fun alreadyRegistered(): String
}