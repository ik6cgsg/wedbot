package wedbot.domain.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import wedbot.BotConstants
import wedbot.domain.entity.Status
import wedbot.domain.repository.UserRepository
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

interface NotificationListener {
    fun stillThinkingAboutEvent(chatId: Long, name: String?)
}

class NotificationService(
    private val userRepository: UserRepository,
    private val listener: NotificationListener
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val zone = ZoneId.of("Europe/Moscow")
    val deadline = LocalDate
        .parse(BotConstants.eventStatusDeadline, formatter)
        .plusDays(1)
        .atStartOfDay(zone)

    fun startDailyReminder() {
        if (ZonedDateTime.now(zone).isAfter(deadline)) {
            // TODO: send all thinking users warning?
            println("time for survey is out")
            return
        }
        scope.launch {
            while (isActive) {
                var targetMskTime: ZonedDateTime = LocalDate.now(zone)
                    .atTime(LocalTime.of(11, 0))
                    .atZone(zone)
                val now = ZonedDateTime.now(zone)
                // TODO: remove
                //targetMskTime = now.plusSeconds(10)
                if (now.isAfter(targetMskTime)) {
                    targetMskTime = targetMskTime.plusDays(1)
                }
                val delayMillis = ChronoUnit.MILLIS.between(now, targetMskTime)
                delay(delayMillis)
                try {
                    processDailyReminders()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun processDailyReminders() {
        val userStatuses = userRepository.getStatuses(null, null)
        userStatuses
            .filter { it.eventStatus == Status.THINKING }
            .forEach { user ->
                listener.stillThinkingAboutEvent(user.chatId, user.name)
            }
    }
}
