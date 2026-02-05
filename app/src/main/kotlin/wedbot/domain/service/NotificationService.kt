package wedbot.domain.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import wedbot.BotConstants
import wedbot.SystemProperties
import wedbot.domain.entity.Status
import wedbot.domain.entity.TransferStatus
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
    fun hasIdleSurveys(chatId: Long)
}

class NotificationService(
    private val userRepository: UserRepository,
    private val listener: NotificationListener
) {
    private val logger = Logger.getLogger(this::class.java.name)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val zone = ZoneId.of("Europe/Moscow")
    val weddingDeadline = LocalDate
        .parse(BotConstants.weddingDeadline, formatter)
        .plusDays(1)
        .atStartOfDay(zone)

    fun startDailyReminder() {
        logger.info("Starting daily reminders...")
        if (SystemProperties.dummy) return
        scope.launch {
            while (isActive) {
                if (ZonedDateTime.now(zone).isAfter(weddingDeadline)) {
                    logger.warning("Notification deadline has passed. Stopping daily reminders...")
                    return@launch
                }
                var targetMskTime: ZonedDateTime = LocalDate.now(zone)
                    .atTime(LocalTime.of(10, 0))
                    .atZone(zone)
                val now = ZonedDateTime.now(zone)
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
        if (BotConstants.eventStatusDeadline.isDatePassedAlready()) {
            logger.info("eventStatusDeadline has passed, ingoring...")
        } else {
            val eventThinkingUsers = userStatuses.filter { it.eventStatus == Status.THINKING }
            logger.info("Found ${eventThinkingUsers.size} users with 'THINKING' status for event. Sending notifications...")
            eventThinkingUsers.forEach { user ->
                listener.stillThinkingAboutEvent(user.chatId, user.name)
            }
        }
        if (BotConstants.villaStatusDeadline.isDatePassedAlready()) {
            logger.info("villaStatusDeadline has passed, ingoring...")
        } else {
            val surveysThinkingUsers = userStatuses.filter {
                it.eventStatus == Status.APPROVED && (it.villaStatus == Status.THINKING || it.transferStatus == TransferStatus.THINKING)
            }
            surveysThinkingUsers.forEach { user ->
                listener.hasIdleSurveys(user.chatId)
            }
        }
        logger.info("Finished processing daily reminders.")
    }

    private fun String.isDatePassedAlready(): Boolean = try {
        val curDate = LocalDate
            .parse(this, formatter)
            .atStartOfDay(zone)
       ZonedDateTime.now(zone).isAfter(curDate)
    } catch(_: Throwable) {
        false
    }
}
