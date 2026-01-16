package wedbot.presentation.util

import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.ReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton

object ReplyMarkupHelper {
    fun shareContactMarkup(label: String) = KeyboardReplyMarkup(
        keyboard = listOf(
            listOf(KeyboardButton(label, requestContact = true)),
        ),
        resizeKeyboard = true
    )

    fun createReplyMarkup(markup: List<List<String>>): ReplyMarkup {
        val keyboardMarkup = mutableListOf<List<KeyboardButton>>()
        markup.forEach { row ->
            keyboardMarkup.add(row.map { label -> KeyboardButton(label) })
        }
        return KeyboardReplyMarkup(keyboard = keyboardMarkup, resizeKeyboard = true)
    }

    @JvmName("createInlineMarkupCallbacks")
    fun createInlineMarkup(callbacks: List<InlineKeyboardButton.CallbackData>): InlineKeyboardMarkup {
        val buttons = callbacks.map { listOf(it) }
        return InlineKeyboardMarkup.create(buttons)
    }

    @JvmName("createInlineMarkupQueryData")
    fun createInlineMarkup(callbacks: List<QueryData>): InlineKeyboardMarkup {
        val buttons = callbacks.map { listOf(it.toCallbackData()) }
        return InlineKeyboardMarkup.create(buttons)
    }
}

data class QueryData(val text: String, val query: String)

fun QueryData.toCallbackData() = InlineKeyboardButton.CallbackData(text, query)