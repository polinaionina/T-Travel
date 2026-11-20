
# Эндпоинты

Формат обмена: JSON. Время в ISO 8601 (UTC). Идентификаторы числовые (как в БД).
Деньги: целое число в минимальных единицах валюты (копейки) и код валюты, например `"price": 4200000, "currency": "RUB"` это 42 000 руб.
Авторизации в проекте нет, пользователь передаётся параметром `userId`.

## 1. API оркестратора (для web)

Базовый путь: корень (`/`), без префикса. Ресурсы: `/offers`, `/orders`, `/notifications`.

### Предложения

| Метод | Путь | Назначение | Запрос | Успешный ответ | Ошибки |
|---|---|---|---|---|---|
| `GET` | `/offers` | Предложения для сборки поездки (оркестратор запрашивает их у поставщика) | query: `type` (`FLIGHT`, `HOTEL`, `INSURANCE`, `TRANSFER`), `from`, `to`, `city`, `dateFrom`, `dateTo` | `200` список предложений | `400`, `503` (поставщик недоступен) |

### Заказы

| Метод | Путь | Назначение | Запрос | Успешный ответ | Ошибки |
|---|---|---|---|---|---|
| `POST` | `/orders` | Создать заказ (статус `DRAFT`) и зафиксировать выбранные предложения | заголовок `Idempotency-Key` (обязателен); тело: `userId`, `components[]` (`type`, `offerId`) | `201` заказ; `200` и тот же заказ, если ключ уже использован с тем же телом | `400`, `409` (ключ уже использован с другим телом) |
| `POST` | `/orders/{id}/submit` | Отправить заказ на подтверждение, запускает процесс | без тела | `202` заказ в статусе `SUBMITTED` | `404`, `409` (недопустимый переход) |
| `GET` | `/orders/{id}` | Заказ со статусами компонентов | - | `200` заказ | `404` |
| `GET` | `/orders` | Список заказов пользователя | query: `userId` (обязателен), `status`, `page`, `size` | `200` страница заказов | `400` |
| `POST` | `/orders/{id}/cancel` | Отмена заказа пользователем | тело: `reason` | `202` заказ в статусе `COMPENSATING` или `CANCELLED` | `404`, `409` |

### Журнал и обновления

| Метод | Путь | Назначение | Запрос | Успешный ответ | Ошибки |
|---|---|---|---|---|---|
| `GET` | `/orders/{id}/events` | Журнал событий заказа для временной шкалы | query: `componentId` (необязательно) | `200` список событий по времени | `404` |
| `GET` | `/orders/{id}/stream` | Обновления заказа в реальном времени (SSE) | - | `200`, `text/event-stream` | `404` |
| `GET` | `/orders/{id}/components/{componentId}/attempts` | Попытки обращения к поставщику по компоненту (для отладки и показа ретраев) | - | `200` список попыток | `404` |

### Уведомления

| Метод | Путь | Назначение | Запрос | Успешный ответ | Ошибки |
|---|---|---|---|---|---|
| `GET` | `/notifications` | Уведомления пользователя | query: `userId` (обязателен), `unread` (`true`/`false`) | `200` список уведомлений | `400` |
| `POST` | `/notifications/{id}/read` | Отметить прочитанным | без тела | `204` | `404` |

## 2. Модели ответов

### Заказ (`GET /orders/{id}`)

```json
{
  "id": 101,
  "userId": 7,
  "status": "CONFIRMING",
  "totalPrice": 8450000,
  "currency": "RUB",
  "failureReason": null,
  "createdAt": "2026-10-16T07:10:00Z",
  "updatedAt": "2026-10-16T07:10:05Z",
  "components": [
    {
      "id": 1001,
      "type": "FLIGHT",
      "stepNo": 1,
      "status": "CONFIRMED",
      "price": 4200000,
      "currency": "RUB",
      "offerId": "fl-2201",
      "externalBookingId": "AL-88231",
      "deadlineAt": "2026-10-16T07:15:00Z",
      "offer": { "from": "SVX", "to": "AER", "departure": "2026-11-02T04:30:00Z" },
      "attemptsCount": 1,
      "lastError": null
    },
    {
      "id": 1002,
      "type": "HOTEL",
      "stepNo": 1,
      "status": "RESERVING",
      "price": 3800000,
      "currency": "RUB",
      "offerId": "ht-0987",
      "externalBookingId": null,
      "deadlineAt": "2026-10-16T07:15:00Z",
      "offer": { "name": "Отель Берег", "city": "Сочи", "nights": 7 },
      "attemptsCount": 2,
      "lastError": "timeout"
    }
  ]
}
```

Поля компонента, которых нет напрямую в таблице:
- `offerId` берётся из `provider_offer_id`, `offer` из `offer_snapshot` (копия предложения на момент выбора, по ней обнаруживается смена цены);
- `stepNo` задаёт порядок шагов: компоненты с одинаковым номером выполняются параллельно, с большим номером после них;
- `attemptsCount` и `lastError` считаются по таблице `ComponentAttempt`.

### Событие журнала (элемент `GET /orders/{id}/events`)

```json
{
  "id": 42,
  "orderId": 101,
  "componentId": 1002,
  "eventType": "STATUS_CHANGED",
  "fromStatus": "RESERVING",
  "toStatus": "FAILED",
  "reason": "Гостиница недоступна",
  "payload": { "supplierError": "503" },
  "createdAt": "2026-10-16T07:10:07Z"
}
```

`componentId` пустой (`null`), если событие относится ко всему заказу. Примерные значения `eventType`: `ORDER_CREATED`, `STATUS_CHANGED`, `RETRY`, `PRICE_CHANGED`, `COMPENSATION_STARTED`, `ORDER_CANCELLED`.

### Попытка (элемент `.../attempts`)

```json
{
  "id": 5,
  "componentId": 1002,
  "operation": "RESERVE",
  "requestId": "101:1002:RESERVE",
  "result": "TIMEOUT",
  "error": "Read timed out",
  "startedAt": "2026-10-16T07:10:05Z",
  "finishedAt": "2026-10-16T07:10:07Z"
}
```

### Уведомление (элемент `GET /notifications`)

```json
{
  "id": 9,
  "userId": 7,
  "orderId": 101,
  "eventId": 42,
  "message": "Гостиница недоступна, заказ отменяется",
  "status": "UNREAD",
  "createdAt": "2026-10-16T07:10:08Z",
  "readAt": null
}
```

### Сообщение SSE (`GET /orders/{id}/stream`)

Каждое событие журнала приходит сразу после записи:

```
event: order-event
data: {"id":42,"orderId":101,"componentId":1002,"eventType":"STATUS_CHANGED","fromStatus":"RESERVING","toStatus":"FAILED","reason":"Гостиница недоступна","createdAt":"2026-10-16T07:10:07Z"}
```

После получения события web обновляет заказ через `GET /orders/{id}`.

## 3. Статусы

| Сущность | Значения |
|---|---|
| Заказ (`OrderStatus`) | `DRAFT`, `SUBMITTED`, `CONFIRMING`, `CONFIRMED`, `COMPENSATING`, `CANCELLED`, `COMPENSATION_FAILED` |
| Компонент (`ComponentStatus`) | `PENDING`, `RESERVING`, `RESERVED`, `CONFIRMED`, `FAILED`, `UNKNOWN`, `COMPENSATING`, `CANCELLED` |
| Тип компонента (`ComponentType`) | `FLIGHT`, `HOTEL`, `INSURANCE`, `TRANSFER` |
| Уведомление | `UNREAD`, `READ` |

Окончательный список статусов определяется диаграммой состояний (state-machine).

## 4. Ошибки

Формат одинаков для всех эндпоинтов:

```json
{
  "code": "ORDER_NOT_FOUND",
  "message": "Заказ не найден",
  "details": {}
}
```

| HTTP | `code` | Когда |
|---|---|---|
| `400` | `VALIDATION_ERROR` | неверные параметры или тело запроса, нет заголовка `Idempotency-Key` |
| `404` | `ORDER_NOT_FOUND`, `NOTIFICATION_NOT_FOUND` | заказ или уведомление не найдены |
| `409` | `INVALID_STATUS_TRANSITION` | недопустимый переход статуса (например, отмена уже подтверждённого заказа) |
| `409` | `IDEMPOTENCY_KEY_CONFLICT` | ключ уже использован с другим телом запроса (не совпал `request_hash`) |
| `409` | `CONCURRENT_MODIFICATION` | заказ изменён параллельно (не совпала `version`), запрос можно повторить |
| `503` | `SUPPLIER_UNAVAILABLE` | поставщик недоступен |
| `500` | `INTERNAL_ERROR` | внутренняя ошибка оркестратора |

## 5. API поставщиков (имитаторы)

Базовый путь: `/{supplier}`, где `supplier`: `airline`, `hotel`, `insurance`, `transfer`. Контракт у всех четырёх одинаковый, отличаются только поля предложений.

| Метод | Путь | Назначение | Запрос | Успешный ответ | Ошибки |
|---|---|---|---|---|---|
| `GET` | `/{supplier}/offers` | Список предложений | query зависит от поставщика | `200` список | `400` |
| `POST` | `/{supplier}/reserve` | Зарезервировать | заголовок `Idempotency-Key`; тело: `offerId`, данные пассажира или гостя | `200` бронь в статусе `RESERVED` | `400`, `409` (предложение недоступно), `503` |
| `POST` | `/{supplier}/confirm/{id}` | Подтвердить бронь | без тела | `200` бронь в статусе `CONFIRMED` | `404`, `409`, `503` |
| `POST` | `/{supplier}/cancel/{id}` | Отменить бронь (компенсация) | без тела | `200` бронь в статусе `CANCELLED` | `404`, `503` |
| `GET` | `/{supplier}/status/{id}` | Текущий статус брони (для разбора `UNKNOWN`) | - | `200` бронь | `404` |

Правила:
- Повторный `reserve` с тем же `Idempotency-Key` возвращает ту же бронь, а не создаёт новую.
- `cancel` можно вызывать повторно безопасно.
- Ключ идемпотентности оркестратор строит детерминированно: `{orderId}:{componentId}:{operation}`. Он же записывается в `ComponentAttempt.requestId`.
- Идентификатор брони у поставщика сохраняется в `OrderComponent.externalBookingId`.

### Управление сбоями

| Метод | Путь | Назначение | Запрос | Успешный ответ |
|---|---|---|---|---|
| `POST` | `/sim/faults/{supplier}` | Включить режим сбоя для поставщика | тело: `mode`, `rate`, `delayMs` | `200` текущая настройка |
| `GET` | `/sim/faults/{supplier}` | Посмотреть текущий режим | - | `200` текущая настройка |
| `DELETE` | `/sim/faults/{supplier}` | Вернуть нормальную работу | - | `204` |

| `mode` | Поведение |
|---|---|
| `OK` | отвечает нормально |
| `FAIL` | возвращает ошибку (`503`) |
| `LATENCY` | отвечает с задержкой `delayMs` |
| `PRICE_CHANGE` | меняет цену при подтверждении |
| `SUCCESS_BUT_TIMEOUT` | выполняет операцию, но не отвечает |

Пример: `POST /sim/faults/hotel` с телом `{"mode": "FAIL", "rate": 1.0}`: любая бронь гостиницы падает.