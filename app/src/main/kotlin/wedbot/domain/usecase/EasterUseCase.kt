package wedbot.domain.usecase

import java.io.File

class EasterUseCase {
    sealed class Result {
        data class Document(val file: File) : Result()
        data class Text(val msg: String) : Result()
        data class RandomSticker(val setName: String) : Result()
        object Dice : Result()
        object Unknown : Result()
    }

    private val depList = listOf("dep", "дэп", "деп")
    private val swagList = listOf("swag", "свег", "свэг", "swaga", "свага")
    private val pepeList = listOf("пэпэ", "ватафа", "шнейне", "фа", "кхекхе", "втфа")
    private val chokoList = listOf("пай", "чокопай", "чоко", "николай", "коля", "pie", "chocopie")

    operator fun invoke(text: String): Result {
        return when (text.lowercase()) {
            in depList -> Result.Dice
            in swagList -> Result.Document(File("res/swaga.jpg"))
            in pepeList -> {
                val wtfa = kotlin.random.Random.nextInt(1, 10)
                val shneyne = List(wtfa) { pepeList.random() }
                Result.Text(shneyne.joinToString(" "))
            }
            in chokoList -> Result.RandomSticker("chokopijca")
            else -> Result.Unknown
        }
    }
}