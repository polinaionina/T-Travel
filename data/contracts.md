# Контракты фронт-бэк

## Формат данных

- Тела запросов и ответов передаются в JSON.
- `id`, `userId`, `orderId` и `componentId` — integer. `offerId` и `externalBookingId` — varchar(255).
- `price` и `totalPrice` — целые числа в минимальных единицах валюты. Например, `4200000` с `currency: RUB` означает 42 000 ₽.
- Метки времени передаются в ISO 8601 в UTC, например `2026-11-02T04:30:00Z`. Календарные даты передаются в формате `YYYY-MM-DD`.
- В текущем API авторизации нет. `userId` передаётся в теле создания заказа и в параметрах списков заказов и уведомлений.

## Предложения

### Запрос поиска

`GET /offers?type=FLIGHT&from=SVX&to=AER&dateFrom=2026-11-02&dateTo=2026-11-09`

| Параметр | Значение |
|---|---|
| `type` | Тип компонента. |
| `from`, `to` | Пункты отправления и назначения для поиска по маршруту. |
| `city` | Город для поиска по месту, например для гостиницы. |
| `dateFrom`, `dateTo` | Границы периода поиска. Набор обязательных параметров зависит от типа предложения. |

### Ответ поиска

```json
{
  "availableOffers": [
    {
      "type": "FLIGHT",
      "offerId": "fl-2201",
      "price": 4200000,
      "currency": "RUB",
      "offer": {
        "from": "SVX",
        "to": "AER",
        "departure": "2026-11-02T04:30:00Z"
      }
    }
  ]
}
```

Каждый элемент описывает одно предложение. Содержимое `offer` зависит от типа. Если подходящих предложений нет, `availableOffers` — пустой массив.

## Заказы

### Создание заказа

`POST /orders` с заголовком `Idempotency-Key: 1ab584b8-028d-4e8b-8af4-2d0884a26915`.

```json
{
  "userId": 7
}
```

Фронтенд генерирует UUID один раз при открытии конструктора и использует его при повторах, в том числе после перезагрузки страницы. Ответ содержит [`OrderView`](#orderview). У нового заказа `status: DRAFT`, `components: []`, `totalPrice: 0`, а `actionRequiredFor` и `holdExpiresAt` равны `null`.

### Выбор предложения

`PUT /orders/101/components/FLIGHT`

```json
{
  "offerId": "fl-2201"
}
```

После успешного резервирования ответ содержит [`OrderView`](#orderview) с компонентом в статусе `RESERVED`.

### Подтверждение и отмена

`POST /orders/101/submit` отправляется без тела. При отмене можно передать причину в `POST /orders/101/cancel`.

```json
{
  "reason": "Планы изменились"
}
```

Оба ответа содержат [`OrderView`](#orderview). Начальный статус после принятия команды зависит от перехода, указанного в [эндпоинтах заказов](endpoints.md#заказы).

### OrderView

Пример после выбора перелёта. Такой же формат используют чтение заказа и команды, которые его изменяют.

```json
{
  "id": 101,
  "userId": 7,
  "status": "DRAFT",
  "totalPrice": 4200000,
  "currency": "RUB",
  "failureReason": null,
  "actionRequiredFor": null,
  "holdExpiresAt": "2026-10-16T07:25:05Z",
  "createdAt": "2026-10-16T07:10:00Z",
  "updatedAt": "2026-10-16T07:10:05Z",
  "components": [
    {
      "id": 1001,
      "type": "FLIGHT",
      "stepNo": 1,
      "status": "RESERVED",
      "price": 4200000,
      "currency": "RUB",
      "offerId": "fl-2201",
      "externalBookingId": "AL-88231",
      "deadlineAt": "2026-10-16T07:25:05Z",
      "offer": {
        "from": "SVX",
        "to": "AER",
        "departure": "2026-11-02T04:30:00Z"
      },
      "attemptsCount": 1,
      "lastError": null
    }
  ]
}
```

- `components` содержит актуальный компонент каждого выбранного типа. При замене создаётся запись с новым `id`. Актуальным считается компонент с наибольшим `id` для своего типа. Предыдущие записи сохраняются для истории.
- `totalPrice` учитывает только актуальные компоненты.
- `stepNo` задаёт последовательность подтверждения. Перелёт — 1, гостиница — 2, страховка — 3, трансфер — 4.
- `offerId` берётся из `provider_offer_id`, `offer` — из `offer_snapshot`. Снимок фиксирует условия на момент выбора.
- `attemptsCount` и `lastError` вычисляются по записям `ComponentAttempt`.
- `actionRequiredFor` содержит тип первого по `stepNo` актуального компонента, который нужно выбрать заново (статус `FAILED` или `CANCELLED` при сборке или восстановлении заказа). Например, при отказе гостиницы фронтенд получает `HOTEL` и открывает её шаг. Если замена не требуется, поле равно `null`.
- `holdExpiresAt` — ближайший `deadlineAt` среди актуальных компонентов в `RESERVED`, либо `null`, если удерживаемых броней нет.

### Список заказов

`GET /orders?userId=7&status=DRAFT&page=0&size=20`

Нумерация страниц начинается с нуля. В примере страница пустая. При наличии заказов `items` содержит объекты [`OrderView`](#orderview).

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalElements": 0
}
```

### Статусы и типы

| Сущность | Значения |
|---|---|
| Заказ | `DRAFT`, `CONFIRMING`, `ACTION_REQUIRED`, `PAYMENT_PENDING`, `PAYMENT_FAILED`, `CONFIRMED`, `CANCELLING`, `NEEDS_HELP`, `CANCELLED` |
| Компонент | `PENDING`, `RESERVING`, `RESERVED`, `CONFIRMED`, `FAILED`, `UNKNOWN`, `COMPENSATING`, `CANCELLED` |
| Тип компонента | `FLIGHT`, `HOTEL`, `INSURANCE`, `TRANSFER` |
| Уведомление | `UNREAD`, `READ` |

## Журнал, обновления и уведомления

### Событие заказа

`GET /orders/{id}/events` возвращает массив событий по времени.

```json
{
  "id": 42,
  "orderId": 101,
  "componentId": null,
  "eventType": "STATUS_CHANGED",
  "fromStatus": "CONFIRMING",
  "toStatus": "ACTION_REQUIRED",
  "reason": "Гостиница недоступна",
  "payload": { "failedComponentId": 1002 },
  "createdAt": "2026-10-16T07:12:07Z"
}
```

`componentId` равен `null` для события всего заказа. Примеры `eventType` — `ORDER_CREATED`, `COMPONENT_RESERVED`, `COMPONENT_CONFIRMED`, `COMPONENT_CONFIRM_FAILED`, `STATUS_CHANGED`, `RETRY`, `PRICE_CHANGED`, `COMPENSATION_STARTED`, `ORDER_CANCELLED`.

### Попытка обращения к поставщику

`GET /orders/{id}/components/{componentId}/attempts` возвращает массив попыток.

```json
{
  "id": 5,
  "componentId": 1002,
  "operation": "CONFIRM",
  "requestId": "101:1002:CONFIRM",
  "result": "TIMEOUT",
  "error": "Read timed out",
  "startedAt": "2026-10-16T07:12:05Z",
  "finishedAt": "2026-10-16T07:12:07Z"
}
```

### Уведомление

`GET /notifications?userId=7&unread=true` возвращает массив уведомлений.

```json
{
  "id": 9,
  "userId": 7,
  "orderId": 101,
  "eventId": 42,
  "message": "Гостиница недоступна, выберите замену",
  "status": "UNREAD",
  "createdAt": "2026-10-16T07:12:08Z",
  "readAt": null
}
```

## Ошибки

Пример ответа `409 Conflict` на выбор недоступного предложения.

```json
{
  "code": "OFFER_UNAVAILABLE",
  "message": "Предложение больше недоступно",
  "details": {
    "type": "HOTEL",
    "offerId": "ht-0987"
  }
}
```

| HTTP | `code` | Когда возвращается |
|---|---|---|
| `400` | `VALIDATION_ERROR` | Неверные параметры, тело запроса или отсутствует обязательный заголовок. |
| `404` | `ORDER_NOT_FOUND`, `COMPONENT_NOT_FOUND`, `NOTIFICATION_NOT_FOUND` | Ресурс не найден. |
| `409` | `INVALID_STATUS_TRANSITION` | Текущий статус заказа не допускает команду. |
| `409` | `ORDER_NOT_READY` | Заказ можно отправить на подтверждение из этого статуса, но состав ещё не готов. |
| `409` | `OFFER_UNAVAILABLE` | Поставщик отклонил резервирование предложения. |
| `409` | `CONCURRENT_MODIFICATION` | Заказ изменился параллельно. Перед повтором можно перечитать его состояние. |
| `503` | `SUPPLIER_UNAVAILABLE` | Оркестратор не смог получить результат от поставщика. При таймауте бронирования оркестратор проверяет статус перед повтором операции. |
| `500` | `INTERNAL_ERROR` | Внутренняя ошибка оркестратора. |

## Пассажиры

В [сценарии](userflow.md#2-страницы-и-что-происходит-в-оркестраторе) данные пассажиров вводятся после выбора компонентов. Формат сохранения пока требует согласования. Черновой вариант — `PUT /orders/{id}/passengers` с полным списком и ответом `204`.

```json
{
  "passengers": [
    {
      "name": "Андреева Алёна Сергеевна",
      "birthDate": "2001-07-29",
      "docsId": "000123456",
      "sex": "Ж",
      "comment": null
    }
  ]
}
```

Нужно согласовать, запрашиваем ли количество пассажиров перед поиском перелёта и гостиницы и сверяем ли его с размером списка на последнем шаге.
