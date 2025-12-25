package wedbot.domain.usecase

import java.io.File

class EasterUseCase {
    sealed class Result {
        data class Document(val file: File) : Result()
        object Dice : Result()
        object Unknown : Result()
    }

    private val depList = listOf("dep", "дэп", "деп")
    private val swagList = listOf("swag", "свег", "свэг", "swaga", "свага")

    operator fun invoke(text: String): Result {
        return when (text.lowercase()) {
            in depList -> Result.Dice
            in swagList -> Result.Document(File("res/swaga.jpg"))
            else -> Result.Unknown
        }
    }
}