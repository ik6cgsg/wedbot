# wedbot

Telegram-бот для координации свадебных гостей.

## Что умеет

- авторизация гостя по `username` Telegram или по отправленному контакту;
- меню с описанием события, календарём, геометкой и дресс-кодом;
- сбор статуса посещения мероприятия;
- дополнительные опросы для подтверждённых гостей с сохранением выбора пользователя:
  - коттедж;
  - трансфер;
  - еда и напитки;
- ежедневные напоминания гостям, которые не завершили нужные шаги;
- административная рассылка по всем пользователям или только подтверждённым гостям;
- административный обзор пользовательских статусов по опросам.

## Стек

- Kotlin 2
- Gradle 8
- [kotlin-telegram-bot](https://github.com/kotlin-telegram-bot/kotlin-telegram-bot)
- Ktor 3
- Exposed + SQLite

## Конфигурация

Локальная конфигурация хранится в `local.properties`. Этот файл уже исключён из git и не должен попадать в релиз.

Создай его на основе примера `local.properties.example`:

```properties
bot.token=123456:replace_me
bot.host=example.com
bot.port=8443
bot.webhook=false
bot.webhook.path=telegram-webhook
keystore.pswd=replace_me
db.init=false
mode.debug=false
mode.dummy=false
```

Пояснения:

- `bot.token` - токен Telegram-бота (via @BotFather);
- `bot.host` - публичный хост HTTPS-сервера для webhook-режима;
- `bot.port` - порт HTTPS-сервера;
- `bot.webhook` - `true` для webhook, `false` для polling;
- `bot.webhook.path` - путь для обработки webhook;
- `keystore.pswd` - пароль keystore для HTTPS;
- `db.init` - `true` для полной очистки и пересоздания БД при старте, для продакшена должно быть `false`;
- `mode.debug` - `true` для расширенного логирование;
- `mode.dummy` - `true` только на время технических работ (`DummyDispatcher`).

## Запуск

Локально:

```bash
./gradlew run
```

Тесты:

```bash
./gradlew test
```

Фоновый запуск на хосте с webhook:

```bash
./run.sh
```

## Структура проекта

- `app/src/main/kotlin/wedbot/App.kt` - композиция приложения;
- `app/src/main/kotlin/wedbot/presentation` - Telegram и webhook-слой;
- `app/src/main/kotlin/wedbot/domain` - use cases, политики и сущности;
- `app/src/main/kotlin/wedbot/data` - SQLite/Exposed и текстовые ресурсы;
- `app/res` - статические ресурсы: изображения, `.ics`, SQL-утилиты.
