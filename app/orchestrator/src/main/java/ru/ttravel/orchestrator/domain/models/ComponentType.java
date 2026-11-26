package ru.ttravel.orchestrator.domain.models;

import java.util.Arrays;
import java.util.List;

public enum ComponentType { 
    FLIGHT(1, true),
    HOTEL(2, true),
    INSURANCE(3, false),
    TRANSFER(4, false);

    private final int stepNo;
    private final boolean required;

    ComponentType(int stepNo, boolean required) {
        this.stepNo = stepNo;
        this.required = required;
    }

    /**
     * @return номер шага (страницы) этого компонента
     */
    public int getStepNo() {
        return stepNo;
    }

    /**
     * @return {@code true}, если компонент обязателен для подтверждения заказа
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * @return список обязательных типов компонентов
     */
    public static List<ComponentType> requiredTypes() {
        return Arrays.stream(values()).filter(ComponentType::isRequired).toList();
    }
}