package ru.ttravel.orchestrator.domain.state;

import ru.ttravel.orchestrator.domain.exceptions.InvalidStatusTransitionException;

import java.util.Map;
import java.util.Set;

/**
 * Общая логика конечного автомата: хранит таблицу допустимых переходов
 * и проверяет, можно ли перейти из одного статуса в другой.
 *
 * @param <S> тип статуса (enum)
 */
public abstract class StateMachine<S extends Enum<S>> {

    private final String subject;
    private final Map<S, Set<S>> transitions;

    /**
     * @param subject     что описывает автомат, для текста ошибки ({@code "Заказ"})
     * @param transitions для каждого статуса набор статусов, в которые из него можно перейти;
     *                    статус без записи считается конечным
     */
    protected StateMachine(String subject, Map<S, Set<S>> transitions) {
        this.subject = subject;
        this.transitions = Map.copyOf(transitions);
    }

    /**
     * Проверяет, допустим ли переход.
     *
     * @param from текущий статус
     * @param to   желаемый статус
     * @return {@code true}, если переход разрешён
     */
    public boolean isAllowed(S from, S to) {
        return allowedFrom(from).contains(to);
    }

    /**
     * Проверяет переход и бросает исключение, если он запрещён.
     *
     * @param from текущий статус
     * @param to   желаемый статус
     * @throws InvalidStatusTransitionException если переход запрещён
     */
    public void assertAllowed(S from, S to) throws InvalidStatusTransitionException {
        if (!isAllowed(from, to)) {
            throw new InvalidStatusTransitionException(subject, from, to);
        }
    }

    /**
     * Возвращает статусы, в которые можно перейти из заданного.
     *
     * @param from текущий статус
     * @return набор допустимых статусов, для конечного статуса пустой
     */
    public Set<S> allowedFrom(S from) {
        return transitions.getOrDefault(from, Set.of());
    }
}