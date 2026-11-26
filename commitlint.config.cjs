module.exports = {
    extends: ['@commitlint/config-conventional'],

    rules: {
        'type-enum': [
            2,
            'always',
            [
                'feat', // новая функциональность
                'fix', // исправление бага
                'docs', // изменения в документации
                'style', // форматирование кода
                'refactor', // рефакторинг
                'perf', // оптимизация производительности
                'test', // добавление тестов
                'build', // изменения в сборке
                'ci', // изменения в CI/CD
                'chore', // рутинные задачи
                'revert', // отказ изменений
            ],
        ],

        'type-case': [2, 'always', 'lower-case'],
        'type-empty': [2, 'never'],

        'scope-case': [2, 'always', 'lower-case'],

        'subject-empty': [2, 'never'],
        'subject-full-stop': [2, 'never', '.'],
        'subject-case': [2, 'never', ['sentence-case', 'start-case', 'pascal-case', 'upper-case']],

        'header-max-length': [2, 'always', 100],
    }
}