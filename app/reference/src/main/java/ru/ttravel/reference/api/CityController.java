package ru.ttravel.reference.api;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/cities")
public class CityController {
    private final CityRepository cities;

    public CityController(CityRepository cities) {
        this.cities = cities;
    }

    @GetMapping
    public List<CityDto> all() {
        return cities.findAll().stream().map(this::toDto).toList();
    }

    @GetMapping("/{id}")
    public CityDto byId(@PathVariable Integer id) {
        return cities.findById(id).map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private CityDto toDto(City c) {
        return new CityDto(c.getId(), c.getCode(), c.getName(),
                c.getCountryCode(), c.getTimezone(), c.getLat(), c.getLon());
    }
}