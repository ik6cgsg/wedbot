package wedbot

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CommandTest {
   @Test
   fun `test Command enum values`() {
       assertEquals("start", Command.START.cmd)
       assertEquals("Пере/Запустить бота", Command.START.description)

       assertEquals("ics", Command.SAVE_CALENDAR.cmd)
       assertEquals("Добавить мероприятие в свой календарь", Command.SAVE_CALENDAR.description)

       assertEquals("status", Command.CHANGE_STATUS.cmd)
       assertEquals("Изменить статус посещения мероприятия", Command.CHANGE_STATUS.description)

       assertEquals("invite", Command.INVITE_GUEST.cmd)
       assertEquals("Пригласить своего +1", Command.INVITE_GUEST.description)
   }
}
