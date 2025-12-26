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
import java.util.logging.Level
import java.util.logging.Logger

interface NotificationListener {
    fun stillThinkingAboutEvent(chatId: Long, name: String?)
}

class NotificationService(
    private val userRepository: UserRepository,
    private val listener: NotificationListener
) {
    private val logger = Logger.getLogger(this::class.java.name)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val zone = ZoneId.of("Europe/Moscow")
    val deadline = LocalDate
        .parse(BotConstants.eventStatusDeadline, formatter)
        .plusDays(1)
        .atStartOfDay(zone)

    fun startDailyReminder() {
        if (ZonedDateTime.now(zone).isAfter(deadline)) {
            logger.warning("Notification deadline has passed. Daily reminders will not be started.")
            return
        }
        logger.info("Starting daily reminders...")
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
                logger.info("Next reminder scheduled for $targetMskTime (in ${delayMillis / 1000} seconds)")
                delay(delayMillis)
                try {
                    processDailyReminders()
                } catch (e: Exception) {
                    logger.log(Level.SEVERE, "Error during daily reminder processing", e)
                }
            }
        }
    }

    private fun processDailyReminders() {
        logger.info("Processing daily reminders...")
        val userStatuses = userRepository.getStatuses(null, null)
        val eventThinkingUsers = userStatuses.filter { it.eventStatus == Status.THINKING }

        logger.info("Found ${eventThinkingUsers.size} users with 'THINKING' status for event. Sending notifications...")
        eventThinkingUsers.forEach { user ->
            listener.stillThinkingAboutEvent(user.chatId, user.name)
        }
        logger.info("Finished processing daily reminders.")
    }
}
