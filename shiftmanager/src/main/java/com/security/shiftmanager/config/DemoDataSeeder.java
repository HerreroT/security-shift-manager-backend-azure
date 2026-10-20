package com.security.shiftmanager.config;

import com.security.shiftmanager.model.Guard;
import com.security.shiftmanager.model.Role;
import com.security.shiftmanager.model.Service;
import com.security.shiftmanager.model.ShiftAssignment;
import com.security.shiftmanager.model.User;
import com.security.shiftmanager.repository.GuardRepository;
import com.security.shiftmanager.repository.ServiceRepository;
import com.security.shiftmanager.repository.ShiftAssignmentRepository;
import com.security.shiftmanager.repository.UserRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads example guards, services and shifts (previous and current month) so the app can be tried
 * right away. Disabled with app.seed-demo-data=false and skipped if guards already exist.
 *
 * Coverage: two neighborhoods with 24 hs coverage (day 08-20 and night 20-08, three guards each
 * rotating) and an office building Monday to Friday 08-16.
 */
// Runs after DataSeeder, so the administrator always exists before the demo supervisor is added.
@Order(2)
@Component
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final LocalTime DAY_START = LocalTime.of(8, 0);
    private static final LocalTime NIGHT_START = LocalTime.of(20, 0);

    private static final String SUPERVISOR_EMAIL = "supervisor@shiftmanager.com";
    private static final String SUPERVISOR_PASSWORD = "supervisor123";

    private final GuardRepository guardRepository;
    private final ServiceRepository serviceRepository;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(
            GuardRepository guardRepository,
            ServiceRepository serviceRepository,
            ShiftAssignmentRepository shiftAssignmentRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.guardRepository = guardRepository;
        this.serviceRepository = serviceRepository;
        this.shiftAssignmentRepository = shiftAssignmentRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (guardRepository.count() > 0) {
            return;
        }

        // Read-only user to try the SUPERVISOR role.
        if (userRepository.findByEmail(SUPERVISOR_EMAIL).isEmpty()) {
            userRepository.save(new User(null, "supervisor", SUPERVISOR_EMAIL,
                    passwordEncoder.encode(SUPERVISOR_PASSWORD), true, Role.SUPERVISOR));
            log.info("Seeded demo supervisor user -> email: {} (development password, see README)", SUPERVISOR_EMAIL);
        }

        List<Guard> guards = guardRepository.saveAll(List.of(
                new Guard(null, "Juan", "Pérez", "30111222", "1155550001", true),
                new Guard(null, "María", "Gómez", "31222333", "1155550002", true),
                new Guard(null, "Carlos", "López", "32333444", "1155550003", true),
                new Guard(null, "Lucía", "Fernández", "33444555", "1155550004", true),
                new Guard(null, "Diego", "Martínez", "34555666", "1155550005", true),
                new Guard(null, "Sofía", "Romero", "35666777", "1155550006", true),
                new Guard(null, "Martín", "Sosa", "36777888", "1155550007", true),
                new Guard(null, "Ana", "Torres", "37888999", "1155550008", false)));

        List<Service> services = serviceRepository.saveAll(List.of(
                new Service(null, "Barrio Belgrano", "Av. Cabildo 2000, Belgrano, CABA",
                        "Barrio cerrado, garita principal. Cobertura 24 hs.", true),
                new Service(null, "Barrio Palermo", "Av. Santa Fe 4500, Palermo, CABA",
                        "Garita de acceso vehicular. Cobertura 24 hs.", true),
                new Service(null, "Edificio Torre Norte", "Av. Corrientes 1200, CABA",
                        "Recepción del edificio de oficinas. Lunes a viernes 08 a 16 hs.", true),
                new Service(null, "Depósito Avellaneda", "Av. Mitre 800, Avellaneda",
                        "Servicio dado de baja.", false)));

        YearMonth currentMonth = YearMonth.now();
        LocalDate from = currentMonth.minusMonths(1).atDay(1);
        LocalDate to = currentMonth.atEndOfMonth();

        List<ShiftAssignment> shifts = new ArrayList<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            int rotation = day.getDayOfYear() % 3;
            addRoundTheClock(shifts, services.get(0), guards.subList(0, 3), day, rotation);
            addRoundTheClock(shifts, services.get(1), guards.subList(3, 6), day, rotation);
            if (day.getDayOfWeek() != DayOfWeek.SATURDAY && day.getDayOfWeek() != DayOfWeek.SUNDAY) {
                shifts.add(shift(guards.get(6), services.get(2), day, DAY_START, 8));
            }
        }
        shiftAssignmentRepository.saveAll(shifts);

        log.info("Seeded demo data: {} guards, {} services, {} shifts ({} to {})",
                guards.size(), services.size(), shifts.size(), from, to);
    }

    /** Day shift 08-20 and night shift 20-08 (next day), rotating among three guards. */
    private static void addRoundTheClock(
            List<ShiftAssignment> shifts, Service service, List<Guard> team, LocalDate day, int rotation) {
        shifts.add(shift(team.get(rotation), service, day, DAY_START, 12));
        shifts.add(shift(team.get((rotation + 2) % 3), service, day, NIGHT_START, 12));
    }

    private static ShiftAssignment shift(Guard guard, Service service, LocalDate day, LocalTime start, int hours) {
        ShiftAssignment shift = new ShiftAssignment();
        shift.setGuard(guard);
        shift.setService(service);
        shift.setStartDateTime(day.atTime(start));
        shift.setEndDateTime(day.atTime(start).plusHours(hours));
        shift.recalculateTotalHours();
        return shift;
    }
}
