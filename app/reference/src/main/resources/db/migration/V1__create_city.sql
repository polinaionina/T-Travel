CREATE TABLE city (
    id           INTEGER PRIMARY KEY,
    code         VARCHAR(8)   NOT NULL UNIQUE,
    name         VARCHAR(255) NOT NULL,
    country_code VARCHAR(2)   NOT NULL,
    timezone     VARCHAR(64)  NOT NULL,
    lat          DOUBLE PRECISION,
    lon          DOUBLE PRECISION
);

INSERT INTO city (id, code, name, country_code, timezone, lat, lon) VALUES
 (1, 'MOW', 'Moscow',           'RU', 'Europe/Moscow',      55.7558, 37.6173),
 (2, 'LED', 'Saint Petersburg', 'RU', 'Europe/Moscow',      59.9343, 30.3351),
 (3, 'AER', 'Sochi',            'RU', 'Europe/Moscow',      43.5855, 39.7231),
 (4, 'KZN', 'Kazan',            'RU', 'Europe/Moscow',      55.7961, 49.1064),
 (5, 'KGD', 'Kaliningrad',      'RU', 'Europe/Kaliningrad', 54.7104, 20.4522),
 (6, 'SVX', 'Yekaterinburg',    'RU', 'Asia/Yekaterinburg', 56.8389, 60.6057);