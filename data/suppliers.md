# Контракты поставщиков и оркестратора

схема:
```
supplier/
├── models/ — общие типы данных
│   ├── SupplierOperationStatus.java - Результат операции: SUCCESS, FAILED, UNKNOWN.
│   ├── SupplierBookingStatus.java - Состояние брони: NOT_FOUND, RESERVED, CONFIRMED, CANCELLED, UNKNOWN.
│   └── SupplierError.java - Информация об ошибке: код и описание.
├── request/ - запросы от оркестратора поставщику
│   ├── ReserveRequest.java - Запрос на резервирование выбранного предложения
│   ├── ConfirmRequest.java - Запрос на окончательное подтверждение бронирования
│   ├── CancelRequest.java - Запрос на отмену бронирования
│   └── BookingStatusRequest.java - Запрос на получение текущего статуса бронирования
└── response/ — ответы поставщика оркестратору
    ├── ReserveResponse.java - Результат резервирования
    ├── ConfirmResponse.java - Результат подтверждения бронирования
    ├── CancelResponse.java - Результат отмены бронирования
    └── BookingStatusResponse.java - Текущее состояние бронирования у поставщика
```