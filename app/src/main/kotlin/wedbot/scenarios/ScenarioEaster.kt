//package wedbot
//
//import com.github.kotlintelegrambot.*
//import com.github.kotlintelegrambot.dispatcher.*
//import com.github.kotlintelegrambot.entities.*
//import com.github.kotlintelegrambot.entities.dice.DiceEmoji
//import wedbot.data.db.DBUtils
//import kotlin.random.Random
//
//object UserMessageEaster {
//    const val sticker = "WOW, крутой стикерпак 🥵"
//}
//
//object TextCommandEaster {
//    val easterDep = listOf("dep", "дэп", "деп")
//}
//
//class ScenarioEaster(
//    bot: Bot,
//    dbUtils: DBUtils
//): Scenario(bot, dbUtils) {
//    override fun handleText(text: String, chatId: Long) {
//        if (text in TextCommandEaster.easterDep) {
//            val cid = ChatId.fromId(chatId)
//            val diceList = listOf(DiceEmoji.Football, DiceEmoji.SlotMachine, DiceEmoji.Bowling, DiceEmoji.Basketball, DiceEmoji.Dartboard, DiceEmoji.Dice)
//            val randomDice = diceList[Random.nextInt(diceList.size)]
//            bot.sendDice(cid, randomDice)
//        }
//    }
//}
