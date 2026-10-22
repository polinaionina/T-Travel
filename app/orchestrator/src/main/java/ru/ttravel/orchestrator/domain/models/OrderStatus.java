package ru.ttravel.orchestrator.domain.models;

public enum OrderStatus {
    DRAFT, 
    SUBMITTED, 
    CONFIRMING, 
    CONFIRMED,
    COMPENSATING, 
    CANCELLED, 
    COMPENSATION_FAILED
}