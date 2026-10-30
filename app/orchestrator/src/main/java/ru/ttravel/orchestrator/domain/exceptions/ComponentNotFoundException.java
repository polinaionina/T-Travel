package ru.ttravel.orchestrator.domain.exceptions;

/**
 * Компонент заказа не найден или не принадлежит указанному заказу (HTTP 404).
 */
public class ComponentNotFoundException extends OrchestratorException {

    /**
     * @param componentId идентификатор компонента, которого нет
     */
    public ComponentNotFoundException(Long componentId) {
        super("COMPONENT_NOT_FOUND", "Компонент не найден: id=" + componentId);
    }
}