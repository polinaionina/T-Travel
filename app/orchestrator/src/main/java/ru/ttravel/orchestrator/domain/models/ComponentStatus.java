package ru.ttravel.orchestrator.domain.models;

public enum ComponentStatus {
    PENDING, 
    RESERVING, 
    RESERVED, 
    CONFIRMED,
    FAILED, 
    UNKNOWN, 
    COMPENSATING, 
    CANCELLED
}