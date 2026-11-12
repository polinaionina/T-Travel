package ru.ttravel.suppliers.insurance.generator;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ru.ttravel.suppliers.insurance.domain.AgeBand;
import ru.ttravel.suppliers.insurance.domain.InsuranceProgram;
import ru.ttravel.suppliers.insurance.repository.AgeBandRepository;
import ru.ttravel.suppliers.insurance.repository.InsuranceProgramRepository;

/**
 * Fills the insurance simulator with test data on startup.
 * Runs only when the database has no programs yet.
 * <p>
 * All amounts are in kopecks, currency is RUB. Insurance has no stock, so
 * unlike flights and hotels there is no availability calendar to generate:
 * only programs and age coefficients.
 */
@Component
public class InsuranceDataGenerator implements ApplicationRunner {

    private static final String CURRENCY = "RUB";
    private static final long KOPECKS_PER_RUBLE = 100;

    private final InsuranceProgramRepository programs;
    private final AgeBandRepository ageBands;

    public InsuranceDataGenerator(InsuranceProgramRepository programs, AgeBandRepository ageBands) {
        this.programs = programs;
        this.ageBands = ageBands;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (programs.count() > 0) {
            return; // already generated
        }
        generateAgeBands();
        generatePrograms();
        generateDemoPrograms();
    }

    /** Bands must not overlap and must cover every age allowed by any program (0..99). */
    private void generateAgeBands() {
        ageBands.saveAll(List.of(
                new AgeBand(0, 17, new BigDecimal("0.80")),
                new AgeBand(18, 39, new BigDecimal("1.00")),
                new AgeBand(40, 59, new BigDecimal("1.30")),
                new AgeBand(60, 75, new BigDecimal("1.80")),
                new AgeBand(76, 99, new BigDecimal("2.50"))));
    }

    private void generatePrograms() {
        programs.saveAll(List.of(
                program("RU_BASIC", "Russia basic", "RU",
                        1_000_000, 40_000, 0, 75,
                        "{\"baggage\":false,\"tripCancellation\":false,\"sports\":false}"),
                program("MED30", "Medical 30 000 EUR", "EUROPE",
                        3_000_000, 70_000, 0, 75,
                        "{\"baggage\":false,\"tripCancellation\":false,\"sports\":false}"),
                program("MED50", "Medical 50 000 EUR", "WORLD",
                        5_000_000, 90_000, 0, 75,
                        "{\"baggage\":true,\"tripCancellation\":false,\"sports\":false}"),
                program("PREMIUM", "Premium all-inclusive", "WORLD",
                        10_000_000, 160_000, 0, 85,
                        "{\"baggage\":true,\"tripCancellation\":true,\"sports\":true}")));
    }

    /**
     * Programs with predictable rejections for demos:
     * <ul>
     *   <li>{@code DEMO_INACTIVE}: not on sale, booking is rejected as unavailable;</li>
     *   <li>{@code DEMO_YOUNG}: only ages 18-30, so an older insured person is rejected.</li>
     * </ul>
     */
    private void generateDemoPrograms() {
        InsuranceProgram inactive = program("DEMO_INACTIVE", "Demo: discontinued program", "WORLD",
                5_000_000, 90_000, 0, 75, null);
        inactive.setActive(false);

        InsuranceProgram young = program("DEMO_YOUNG", "Demo: ages 18-30 only", "WORLD",
                5_000_000, 90_000, 18, 30, null);

        programs.saveAll(List.of(inactive, young));
    }

    private InsuranceProgram program(String code, String name, String area,
                                 long coverageRubles, long dailyRateKopecks,
                                 int minAge, int maxAge, String options) {
        return new InsuranceProgram(code, name, area,
                coverageRubles * KOPECKS_PER_RUBLE,
                dailyRateKopecks,
                CURRENCY, minAge, maxAge, options, true);
    }
}