package ru.ttravel.orchestrator.domain.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.ttravel.orchestrator.domain.data.ComponentAttempt;
import ru.ttravel.orchestrator.domain.models.AttemptOperation;

import java.util.List;
import java.util.Optional;

/**
 * Доступ к таблице {@code component_attempt}: попытки обращения к поставщикам.
 */
public interface ComponentAttemptRepository extends JpaRepository<ComponentAttempt, Long> {

    /**
     * Возвращает все попытки по компоненту от первой к последней.
     *
     * @param componentId идентификатор компонента
     * @return список попыток
     */
    List<ComponentAttempt> findByComponentIdOrderByStartedAtAsc(Long componentId);

    /**
     * Возвращает самую последнюю попытку по компоненту.
     * 
     * По ней определяется {@code lastError} в ответе для web.
     *
     * @param componentId идентификатор компонента
     * @return последняя попытка или пустой результат, если попыток ещё не было
     */
    Optional<ComponentAttempt> findFirstByComponentIdOrderByStartedAtDesc(Long componentId);

    /**
     * Считает все попытки по компоненту (поле {@code attemptsCount}).
     *
     * @param componentId идентификатор компонента
     * @return количество попыток
     */
    long countByComponentId(Long componentId);

    /**
     * Считает попытки конкретной операции по компоненту.
     * 
     * Нужен, чтобы ограничить число повторов, например не больше трёх {@code RESERVE}.
     *
     * @param componentId идентификатор компонента
     * @param operation   тип операции
     * @return количество попыток этой операции
     */
    long countByComponentIdAndOperation(Long componentId, AttemptOperation operation);
}