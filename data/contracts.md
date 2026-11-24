**Предложения**

GET /offers - предложения по датам и месту
Request:
{
    "type": varchar(10),
    "from": {
        "city" : varchar(255),
        "date" : timestamp
    },
    "to": {
        "city" : varchar(255),
        "date" : timestamp
    }
}

Response:
{
    "availableOffers": [
        {
            type: varchar(10),
            offerId: [uuid/int]  //массив id предлагаемых компонентов
        }
    ]
}




**Заказы**
POST /orders

Request:
{
  "idempotencyKey": 101,
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
      "status": "ACTION_REQUIRED",
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
      "status": "PAYMENT_PENDING",
      "price": 3800000,
      "currency": "RUB",
      "offerId": "ht-0987",
      "externalBookingId": null,
      "deadlineAt": "2026-10-16T07:15:00Z",
      "offer": { "name": "Отель Берег", "city": "Сочи", "nights": 7 },
      "attemptsCount": 2,
      "lastError": "timeout"
    }
  ],
  "passengers":[
    {
        "name": "Андреева Алена Сергеевна",
        "birthDate": "2001-07-29",
        "docsId": 946830986,
        "sex": "Ж",
        "comment": null
    },
    {
        "name": "Андреев Дмитрий Антонович",
        "birthDate": "1997-11-25",
        "docsId": 94698986,
        "sex": "М",
        "comment": null
    },
  ]
}

Response:
{
    "result": "OK"
}

**Поставщики**

POST /{supplier}/reserve, где supplier: airline, hotel, insurance, transfer.
Это метод будет резервировать компоненты на стороне имитатора. Это не фронт-бэк

Request:
{
  "idempotencyKey": 101,
  "userId": 7,
  "status": "CONFIRMING",
  "totalPrice": 8450000,
  "currency": "RUB",
  "failureReason": null,
  "createdAt": "2026-10-16T07:10:00Z",
  "updatedAt": "2026-10-16T07:10:05Z",
  "component": {
      "id": 1001,
      "stepNo": 1,
      "status": "PAYMENT_PENDING",
      "price": 4200000,
      "currency": "RUB",
      "offerId": "fl-2201",
      "externalBookingId": "AL-88231",
      "deadlineAt": "2026-10-16T07:15:00Z",
      "offer": { "from": "SVX", "to": "AER", "departure": "2026-11-02T04:30:00Z" },
      "attemptsCount": 1,
      "lastError": null
  },
  пассажиры...
}

Короче, нужно запрашивать в начале составления поездки количество пассажиров (чтобы корректно считать места в отелях и самолете), и в конце давать указывать данные только для этого кол-ва пользователей???
