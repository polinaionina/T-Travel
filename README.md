# T-Travel
## Структура проекта
t-travel/
├── app/                                  # Maven multi-module, Java 25
│   ├── pom.xml                           # родительский: версии, плагины, список модулей
│   ├── contracts/                        # DTO и интерфейсы, общие для всех
│   │   └── pom.xml
│   ├── common/                           # общие утилиты
│   │   └── pom.xml
│   ├── orchestrator/                     # главный сервис
│   │   ├── pom.xml
│   │   └── src/
│   │       ├── main/java/ru/ttravel/orchestrator/
│   │       │   ├── api/
│   │       │   ├── domain/
│   │       │   ├── providers/
│   │       │   └── notification/
│   │       ├── main/resources/
│   │       │   ├── application.yml
│   │       │   └── db/migration/         # Потом добавиться, как соединим с бд
│   │       └── test/java/...             # Тож потом
│   └── suppliers/                        # имитаторы внешних поставщиков
│       ├── pom.xml
│       └── src/main/java/ru/ttravel/suppliers/
│           ├── airline/
│           ├── hotel/
│           ├── insurance/
│           ├── transfer/
│           └── faults/
├── web/                                  # React
├── contracts/openapi/                    # YAML-описания API
├── infra/
│   ├── docker-compose.yml
│   └── seed/
├── docs/
├── .gitignore
├── .env.example                          # Потом добавлю
└── README.md