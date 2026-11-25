# ⚡ quest-journal

Квест-журнал: заметки в виде квестов с напоминаниями, журналом обновлений и синхронизацией
между ПК (Windows) и телефоном (Android).

```
 📱 Flutter (Android)  ─┐   REST + (скоро) WebSocket
                        ├──────────────►  ☕ Spring Boot API  ──►  🐘 PostgreSQL
 💻 Flutter (Windows)  ─┘
```

## Статус

| Акт | Что | Статус |
|---|---|---|
| I | Backend: auth, квесты, журнал, sync, тесты | ✅ |
| II | Flutter-клиент (Android + Windows) | ⏳ |
| III | Realtime (WebSocket/STOMP) | ⏳ |
| IV | Напоминания (локальные уведомления) | ⏳ |
| V | Офлайн-режим + FCM | ⏳ |
| VI | Деплой | ⏳ |

## Backend

**Стек:** Java 21 · Spring Boot 4.1 · Spring Security (JWT, HS256) · Spring Data JPA · Flyway ·
PostgreSQL 17 · springdoc (Swagger UI) · Testcontainers.

### Запуск локально

Нужны JDK 21 и Docker.

```bash
docker compose up -d db          # PostgreSQL на localhost:5432
cd backend
./mvnw spring-boot:run           # Windows: mvnw.cmd spring-boot:run
```

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html. Залогинься через `/api/auth/login`,
  скопируй `token` и вставь его в кнопку **Authorize**.

Всё сразу в Docker (БД + backend): `docker compose up -d --build`.

### Тесты

```bash
cd backend
./mvnw test      # поднимает настоящий PostgreSQL через Testcontainers, нужен Docker
```

### Переменные окружения

| Переменная | По умолчанию | Описание |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/quest_journal` | JDBC URL |
| `DB_USER` / `DB_PASSWORD` | `quest` / `quest` | Доступ к БД |
| `JWT_SECRET` | dev-ключ | ⚠️ **Обязательно** задать на сервере, минимум 32 байта. Сгенерировать: `openssl rand -base64 48` |
| `JWT_TTL` | `30d` | Срок жизни токена |
| `INVITE_CODE` | пусто | Если задан, регистрация возможна только с этим кодом. ⚠️ Задай на публичном сервере, иначе зарегистрироваться сможет кто угодно |
| `PORT` | `8080` | HTTP-порт |

## API

Все эндпоинты, кроме register/login/health, требуют `Authorization: Bearer <token>`.
Каждый пользователь видит только свои квесты.

| Метод | Путь | Что делает |
|---|---|---|
| `POST` | `/api/auth/register` | Регистрация `{username, displayName, password, inviteCode}` → токен |
| `POST` | `/api/auth/login` | Вход `{username, password}` → токен |
| `GET` | `/api/auth/me` | Текущий пользователь |
| `GET` | `/api/quests?status=ACTIVE&status=IN_PROGRESS` | Список квестов (фильтр необязателен) |
| `GET` | `/api/quests/{id}` | Квест + весь журнал |
| `POST` | `/api/quests` | Создать квест: 201, повтор с тем же `id` даёт 200 |
| `PUT` | `/api/quests/{id}` | Изменить квест (нужен `version`), при устаревшей версии 409 |
| `DELETE` | `/api/quests/{id}` | Удалить (soft delete) |
| `POST` | `/api/quests/{id}/log` | Дописать обновление в журнал квеста |
| `GET` | `/api/sync?since=<serverTime>` | Изменения с прошлой синхронизации |

### Модель

- **Квест:** `title`, `description`, `type` (`MAIN` / `SIDE`), `status`
  (`ACTIVE` → `IN_PROGRESS` → `COMPLETED` / `FAILED`), `remindAt` (`null` = без напоминания), `version`.
- **Журнал квеста** (append-only): `CREATED`, `NOTE` (твои обновления), `STATUS_CHANGE` (пишется
  автоматически при смене статуса). Записи никогда не редактируются, поэтому правки с двух устройств
  не конфликтуют.

### Как устроена синхронизация

1. **UUID генерирует клиент.** Квест, созданный офлайн, сразу имеет `id`, а повторная отправка того
   же запроса не создаёт дубликат.
2. **Optimistic locking.** `PUT` передаёт `version`, которую клиент видел последней. Если квест
   успели изменить на другом устройстве, сервер вернёт `409` с актуальным состоянием в поле `current`.
3. **Инкрементальный sync.** Клиент хранит `serverTime` из прошлого ответа `/api/sync` и передаёт его
   как `since`. В ответе приходят изменённые квесты (удалённые с `deletedAt`, их нужно убрать локально)
   и новые записи журнала. Сервер берёт окно с запасом в 5 секунд, клиент убирает дубли по `id`.

Пример:

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"hozyain","password":"secret-pass"}' | jq -r .token)

curl -s -X POST localhost:8080/api/quests -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"title":"Сдать курсовую","type":"MAIN","remindAt":"2026-12-10T13:00:00Z"}'
```
