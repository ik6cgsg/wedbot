package wedbot.data.resources

object BotMessages {
    const val INTERNAL_ERROR = "Что-то пошло совсем не так, попробуй связаться с организаторами."
    // start & contact commands
    const val GREETING = """
        Привет, наш дорогой гость%s! 💋
        Мы снова на связи! Как и обещали, в 2026 году состоится вторая часть нашей свадьбы.
        А этот бот снова поможет тебе узнать расписание, место проведения и другую полезную информацию.
    """
    const val INFO_MESSAGE = """
        Дата и место проведения праздника:
        📆 27 марта 2026 года
        📍 Пушкинская усадьба (посёлок Фёдоровское, улица Зелёная, дом 1)
        
        План на день:
        🕓 *сбор* гостей - 16:00
        🕔 *официальная* часть - 17:00
        🕚 *окончание* торжества - 23:00
        
        ‼️ Очень просим указать статус до *%s*, так как это очень важно для организации свадьбы.
        
        🙏 Обязательно заглядывай в наше /menu за интересностями
        
        ⚠️ Не удаляй бота, это наш оперативный способ связи, мы еще вернемся с вопросами про предпочтения
    """
    const val HELP_MESSAGE = """
        Если что-то пошло не так или у вас есть вопросы, вы всегда можете связаться с админами
        
        Илья - @cgsgilich
        Анастасия - @dergoleem
    """
    const val ASK_PHONE = "Пожалуйста, отправь свой номер телефона, чтобы я мог найти тебя в списке гостей."
    const val ASK_PHONE_LABEL = "Отправить телефон"
    const val USER_NOT_FOUND = "К сожалению, я не нашел тебя в списке гостей. Попробуй связаться с организаторами."
    const val WEAK_RIGHTS = "Кажется, у меня не хватает прав на такое..."
    const val USER_ALREADY_REGISTERED = "Этот номер телефона уже используется другим пользователем."
    // Event status
    const val EVENT_STATUS_PING_FIRST = "Подскажи, сможешь ли прийти? Нам важно знать количество гостей."
    const val EVENT_STATUS_PING_DAILY = """
        Привет, %s! 
        Надеюсь, ты уже обдумал, сможешь ли прийти?
    """
    const val EVENT_STATUS_ACCEPT_BUTTON = "Я буду 💯"
    const val EVENT_STATUS_REJECT_BUTTON = "Не смогу ☹️"
    const val EVENT_STATUS_THINK_BUTTON = "Пока думаю 🤔"
    const val EVENT_STATUS_ACCEPTED = "Супер! Мы записали, что ты будешь."
    const val EVENT_STATUS_REJECTED = "Эх, нам очень жаль :(("
    const val EVENT_STATUS_THINK_AGAIN = """
        Большая просьба определиться до %s!
        Напомним завтра или ты можешь обновить статус через /menu
    """
    const val MENU_MESSAGE = """
        Рядом с клавиатурой появилось меню. 
        Там можно найти дополнительную информацию о мероприятии.
    """
    const val MENU_UPDATED = "Меню обновлено! Не забудь заглянуть 😉"
    const val CALENDAR_MESSAGE = """
        Теперь ты можешь сохранить этот файлик себе в календарь!
        
        p.s. на iOS не так просто... для добавления необходимо:
        1. Скачать файл в Файлы
        2. Зажать пальцем скачанный файл в Файлах
        3. Открыть Календарь, не отпуская при этом ics-файл
        4. Отпустить файлик в любом месяце (нужно, чтобы Календарь отображал именно один месяц)
    """
    const val ADMIN_PING_STARTED = "Режим рассылки активирован"
    const val ADMIN_PING_PROMPT = "Введите текст сообщения для рассылки *всем* гостям (или нажмите кнопку ниже для отмены)"
    const val ADMIN_PING_CANCEL = "Рассылка отменена"
    const val ADMIN_PING_CANCEL_BUTTON = "Отменить рассылку"
    const val ADMIN_PING_SUCCEED = "Рассылка успешно завершена"
    const val ADMIN_MSG_HEADER = "⚠️Сообщение от администрации ⚠️"
    const val MENU_BUTTON_INFO = "Информация о празднике"
    const val MENU_BUTTON_ICS = "Календарик"
    const val MENU_BUTTON_LOCATION = "Геометка"
    const val MENU_BUTTON_EVENT_STATUS = "Установить статус посещения мероприятия"
    const val MENU_BUTTON_HELP = "Связаться с организаторами"
    const val MENU_BUTTON_STATUS_TABLE = "Таблица со статусами"
    const val MENU_BUTTON_PING_GUESTS ="Отправить гостям сообщение"
}
