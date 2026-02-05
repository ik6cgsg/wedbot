package wedbot.data.resources

object BotMessages {
    const val INTERNAL_ERROR = "Что-то пошло совсем не так, попробуй связаться с организаторами."
    const val TOTAL_SLEEVE = "Кажется ты по ошибке отказался от нашего мероприятия, попробуй связаться с организаторами."
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
    const val STATUS_THINK_AGAIN = """
        Большая просьба определиться до %s !
        Напомним завтра или можешь пройти опрос в /menu
    """
    // Menu
    const val MENU_MESSAGE = """
        Рядом с клавиатурой появилось меню. 
        Там можно найти дополнительную информацию о мероприятии.
    """
    const val MENU_UPDATED = "_Меню обновлено! Не забудь заглянуть 😉_"
    const val MENU_BUTTON_INFO = "Общая информация"
    const val MENU_BUTTON_ICS = "Календарик"
    const val MENU_BUTTON_LOCATION = "Геометка"
    const val MENU_BUTTON_EVENT_STATUS = "Установить статус посещения мероприятия"
    const val MENU_BUTTON_HELP = "Связаться с организаторами"
    const val MENU_BUTTON_STATUS_TABLE = "Таблица со статусами"
    const val MENU_BUTTON_PING_GUESTS ="Отправить гостям сообщение"
    const val MENU_BUTTON_DRESS_CODE ="ДрессКот"
    const val MENU_BUTTON_VILLA_STATUS = "Коттедж"
    const val MENU_BUTTON_TRANSFER = "Трансфер"
    const val MENU_HAS_SURVEYS = """
        Дорогой гость! Кажется, у тебя есть непройденные опросы...
        Пожалуйста, проверь в /menu кнопки, помеченные знаком ⚠️
    """
    const val MENU_SURVEYS_DOUBLE_CLICK = "_p.s. все пройденные опросы сохраняются в меню и повторное нажатие отображает дополнительную/сохранённую информацию об опросе_"
    const val CALENDAR_MESSAGE = """
        Теперь ты можешь сохранить этот файлик себе в календарь!
        
        p.s. на iOS не так просто... для добавления необходимо:
        1. Скачать файл в Файлы
        2. Зажать пальцем скачанный файл в Файлах
        3. Открыть Календарь, не отпуская при этом ics-файл
        4. Отпустить файлик в любом месяце (нужно, чтобы Календарь отображал именно один месяц)
    """
    // Admin
    const val ADMIN_PING_STARTED = "Режим рассылки активирован"
    const val ADMIN_PING_ALL_PROMPT = """
        Введите текст для рассылки *всем* участникам бота
        (поменять/отменить режим можно по кнопкам ниже)
    """
    const val ADMIN_PING_ALL_BUTTON = "Отправить всем пользователям"
    const val ADMIN_PING_GUESTS_PROMPT = """
        Введите текст для рассылки *согласившимся* гостям
        (поменять/отменить режим можно по кнопкам ниже)
    """
    const val ADMIN_PING_GUESTS_BUTTON = "Отправить только гостям"
    const val ADMIN_PING_CANCEL = "Рассылка отменена"
    const val ADMIN_PING_CANCEL_BUTTON = "Отменить рассылку"
    const val ADMIN_PING_SUCCEED = "Рассылка успешно завершена"
    const val ADMIN_MSG_HEADER = "⚠️Сообщение от администрации ⚠️"
    const val TECH_WORKS = "🛠️ Извини, я пока не работаю... (ведутся технические работы) ((добавляются новые баги)) 🛠️"
    // Villa status
    const val VILLA_STATUS_PING_APPROVED = """
        Здорово, что ты с нами до конца!
        Вся информация о коттедже [здесь](https://vk.com/album-88705863_217214900)
    """
    const val VILLA_STATUS_PING_REJECTED = """
        Дорогой гость!
        Кажется, ты отказался от ночёвки или в коттедже не хватило места :(
        Если захочешь, ты сможешь потусить с нами до 02:00 ночи (далее ворота на территорию закрываются до утра).
        
        Если вдруг есть вопросы, обратись к админам!`
    """
    const val VILLA_STATUS_PING_THINKING = """
        Если ты читаешь это сообщение, значит ты попал в список людей, которых мы мечтаем увидеть в коттедже после банкета!
        Хочешь ли ты остаться на ночь?
        *Важно*: расположение по комнатам по *три* человека
        Подробнее про условия можно посмотреть [здесь](https://vk.com/album-88705863_217214900)
        
        Просьба определиться до *%s*
    """
    const val VILLA_STATUS_ACCEPT_BUTTON = "Хочу и могу!!"
    const val VILLA_STATUS_REJECT_BUTTON = "Не получится (("
    const val VILLA_STATUS_THINK_BUTTON = "Пока подумаю..."
    const val VILLA_STATUS_ACCEPTED = "Ура!!! Королевской ночи быть 🙏"
    const val VILLA_STATUS_REJECTED = "Жаль, что у тебя не получится остаться с нами ☹️"
    // Transfer status
    const val TRANSFER_STATUS_PING_THINKING = """
        Мы очень хотим понять, насколько нужен гостям трансфер.
        Площадка располагает вместительной охраняемой парковкой до 30 авто!
        Пожалуйста, подскажи твой вайб относительно трансфера.
        
        Просьба определиться до *%s*
    """
    const val TRANSFER_STATUS_PING_NEED = "Ты указал, что тебе нужен трансфер. Мы в процессе организации"
    const val TRANSFER_STATUS_PING_SELF_HANDLE = "Спасибо, что сможешь добраться самостоятельно!"
    const val TRANSFER_STATUS_PING_SOCIAL_LEGEND = "Ты просто живая легенда! Скоро обратимся к тебе с возможными попутчиками"
    const val TRANSFER_STATUS_THINKING_BUTTON = "Пока подумаю..." 
    const val TRANSFER_STATUS_NEED_BUTTON = "Хочется телепортации"
    const val TRANSFER_STATUS_SELF_HANDLE_BUTTON = "Спасибо, доберусь сам"
    const val TRANSFER_STATUS_SOCIAL_LEGEND_BUTTON = "Доеду сам и готов подвезти"
    const val TRANSFER_STATUS_NEED_CHOICE = "Спасибо за ответ! Отметим, что необходим трансфер"
    const val TRANSFER_STATUS_SELF_HANDLE_CHOICE = "Спасибо за ответ! Отметим, что доберешься самостоятельно"
    const val TRANSFER_STATUS_SOCIAL_LEGEND_CHOICE = "Спасибо за ответ! И большой респект за помощь!"
}
