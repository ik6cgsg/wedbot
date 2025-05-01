package wedbot

enum class Command(val cmd: String, val description: String) {
    START(
        "start",
        "Пере/Запустить бота"
    ),
    SAVE_CALENDAR(
        "ics",
        "Добавить мероприятие в свой календарь"
    ),
    CHANGE_STATUS(
        "status",
        "Изменить статус посещения мероприятия"
    ),
    INVITE_GUEST(
        "invite",
        "Пригласить своего +1"
    )
}
