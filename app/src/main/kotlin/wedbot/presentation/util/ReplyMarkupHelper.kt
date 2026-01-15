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

    fun createInlineMarkup(callbacks: List<InlineKeyboardButton.CallbackData>): InlineKeyboardMarkup {
        val buttons = callbacks.map { listOf(it) }
        return InlineKeyboardMarkup.create(buttons)
    }
}