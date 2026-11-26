package ru.ttravel.contracts.reference;

public record CityDto(Integer id, String code, String name,
                      String countryCode, String timezone,
                      Double lat, Double lon) {}