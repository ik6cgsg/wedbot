package wedbot.domain.usecase

import wedbot.domain.entity.Status
import wedbot.domain.entity.TransferStatus
import wedbot.domain.entity.UserStatus
import wedbot.domain.policy.canViewAdminPanel
import wedbot.domain.repository.TextRepository
import wedbot.domain.repository.UserRepository

class StatusTableUseCase(
    private val userRepository: UserRepository,
    private val textRepository: TextRepository
) {
    companion object {
        const val PAGE_SIZE = 20
        const val CALLBACK_PREFIX = "status_table_page_"
    }

    sealed class Result {
        data class Table(
            val tableText: String,
            val hasPreviousPage: Boolean,
            val hasNextPage: Boolean,
            val currentPage: Int
        ) : Result()
        data class Error(val msg: String) : Result()
    }

    operator fun invoke(chatId: Long, page: Int = 0): Result {
        val user = userRepository.getByChatId(chatId).getOrElse {
            return Result.Error(textRepository.userNotFound())
        }
        if (!user.canViewAdminPanel()) {
            return Result.Error(textRepository.weakRights())
        }
        val offset = (page * PAGE_SIZE).toLong()
        // PAGE_SIZE + 1 to check if there are more pages
        val statuses = userRepository.getStatuses(offset, PAGE_SIZE + 1)
        val hasNextPage = statuses.size > PAGE_SIZE
        val pageItems = statuses.take(PAGE_SIZE)
        if (pageItems.isEmpty() && page == 0) {
            return Result.Error(textRepository.internalError())
        }
        val tableText = buildMarkdownTable(pageItems)
        return Result.Table(
            tableText = tableText,
            hasPreviousPage = page > 0,
            hasNextPage = hasNextPage,
            currentPage = page
        )
    }

    private fun buildMarkdownTable(statuses: List<UserStatus>): String {
        val header = "|username       |name           |event|villa|trans|food|\n"
        val separator = "|${"-".repeat(15)}|${"-".repeat(15)}|-----|-----|-----|----|\n"
        val rows = statuses.joinToString("\n") { status ->
            val username = (status.username ?: "N/A").take(15).padEnd(15)
            val name = (status.name ?: "N/A").take(15).padEnd(15)
            val event = status.eventStatus.toSymbol().padEnd(5)
            val villa = status.villaStatus.toSymbol().padEnd(5)
            val trans = status.transferStatus.toSymbol().padEnd(5)
            val foodChosen = if (status.foodDrinkChosen) "+" else "-"
            "|$username|$name|$event|$villa|$trans|$foodChosen"
        }
        return "```\n$header$separator$rows\n```"
    }

    private fun Status.toSymbol(): String {
        return when (this) {
            Status.APPROVED -> "+"
            Status.SLEEVE -> "-"
            Status.THINKING -> "?"
        }
    }

    private fun TransferStatus.toSymbol(): String {
        return when (this) {
            TransferStatus.THINKING -> "?"
            TransferStatus.NEED -> "h"
            TransferStatus.SELF_HANDLE -> "+"
            TransferStatus.SOCIAL_LEGEND -> "++"
        }
    }
}
