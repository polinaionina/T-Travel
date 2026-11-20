package ru.ttravel.orchestrator.booking.models;

/**
 * Возможные результаты разрешения неопределённого состояния компонента.
 */
public enum UnknownResolution {
    RESERVED,
    CONFIRMED,
    NOT_FOUND,
    STILL_UNKNOWN
}