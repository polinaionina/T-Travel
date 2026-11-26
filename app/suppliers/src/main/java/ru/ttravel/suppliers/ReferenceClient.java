package ru.ttravel.suppliers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import ru.ttravel.contracts.reference.CityDto;
import java.util.List;
import org.springframework.web.client.RestClient;

@Component
public class ReferenceClient {
    private final RestClient http;

    public ReferenceClient(@Value("${reference.url}") String url) {
        this.http = RestClient.create(url);
    }

    public List<CityDto> loadCities() {
        for (int attempt = 1; attempt <= 10; attempt++) {
            try {
                return http.get().uri("/cities").retrieve()
                        .body(new ParameterizedTypeReference<List<CityDto>>() {});
            } catch (RestClientException e) {
                try { Thread.sleep(2000); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ie);
                }
            }
        }
        throw new IllegalStateException("Reference service is unavailable");
    }
}