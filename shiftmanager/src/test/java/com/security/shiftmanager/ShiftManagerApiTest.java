package com.security.shiftmanager;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "app.seed-demo-data=false")
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ShiftManagerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private long guardId;
    private long serviceId;

    @BeforeEach
    void setUp() throws Exception {
        guardId = createGuard("Juan", "Pérez", "30111222");
        serviceId = createService("Barrio Belgrano", "Av. Cabildo 2000");
    }

    @Test
    void nightShiftCrossingMidnightCalculatesTwelveHours() throws Exception {
        createShift(guardId, serviceId, "2026-10-12T20:00", "2026-10-13T08:00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalHours").value(12.0))
                .andExpect(jsonPath("$.guard.lastName").value("Pérez"))
                .andExpect(jsonPath("$.service.name").value("Barrio Belgrano"));
    }

    @Test
    void onDutyReturnsTheGuardWorkingAtThatMoment() throws Exception {
        createShift(guardId, serviceId, "2026-10-12T08:00", "2026-10-12T20:00");
        long nightGuard = createGuard("Carlos", "López", "32333444");
        createShift(nightGuard, serviceId, "2026-10-12T20:00", "2026-10-13T08:00");

        mockMvc.perform(get("/api/services/{id}/on-duty", serviceId).param("at", "2026-10-12T16:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].guard.firstName").value("Juan"));

        mockMvc.perform(get("/api/services/{id}/on-duty", serviceId).param("at", "2026-10-13T03:00"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].guard.firstName").value("Carlos"));

        // 20:00 belongs to the night shift only: the end of a shift is exclusive.
        mockMvc.perform(get("/api/services/{id}/on-duty", serviceId).param("at", "2026-10-12T20:00"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].guard.firstName").value("Carlos"));
    }

    @Test
    void onDutyIsEmptyWhenNobodyWasAssigned() throws Exception {
        mockMvc.perform(get("/api/services/{id}/on-duty", serviceId).param("at", "2026-10-12T16:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void onDutyForUnknownServiceIsNotFound() throws Exception {
        mockMvc.perform(get("/api/services/{id}/on-duty", 9999).param("at", "2026-10-12T16:00"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void overlappingShiftsAreRejectedButBackToBackAreAllowed() throws Exception {
        createShift(guardId, serviceId, "2026-10-12T08:00", "2026-10-12T20:00")
                .andExpect(status().isCreated());

        long otherService = createService("Barrio Palermo", "Av. Santa Fe 4500");
        createShift(guardId, otherService, "2026-10-12T18:00", "2026-10-12T22:00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());

        createShift(guardId, otherService, "2026-10-12T20:00", "2026-10-13T08:00")
                .andExpect(status().isCreated());
    }

    @Test
    void shiftMustEndAfterItStarts() throws Exception {
        createShift(guardId, serviceId, "2026-10-12T20:00", "2026-10-12T08:00")
                .andExpect(status().isBadRequest());
    }

    @Test
    void inactiveGuardCannotGetNewShifts() throws Exception {
        mockMvc.perform(delete("/api/guards/{id}", guardId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        createShift(guardId, serviceId, "2026-10-12T08:00", "2026-10-12T20:00")
                .andExpect(status().isConflict());
    }

    @Test
    void guardHoursSumsTheShiftsThatStartInTheRange() throws Exception {
        createShift(guardId, serviceId, "2026-09-30T20:00", "2026-10-01T08:00");
        createShift(guardId, serviceId, "2026-10-01T20:00", "2026-10-02T08:00");
        createShift(guardId, serviceId, "2026-10-15T08:00", "2026-10-15T14:30");
        createShift(guardId, serviceId, "2026-10-31T20:00", "2026-11-01T08:00");

        mockMvc.perform(get("/api/guards/{id}/hours", guardId)
                        .param("from", "2026-10-01")
                        .param("to", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shiftCount").value(3))
                .andExpect(jsonPath("$.totalHours").value(30.5))
                .andExpect(jsonPath("$.shifts", hasSize(3)));
    }

    @Test
    void guardHoursWithEmptyRangeIsZero() throws Exception {
        mockMvc.perform(get("/api/guards/{id}/hours", guardId)
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31"))
                .andExpect(jsonPath("$.shiftCount").value(0))
                .andExpect(jsonPath("$.totalHours").value(0.0));
    }

    @Test
    void monthlyReportListsEveryActiveGuard() throws Exception {
        createGuard("María", "Gómez", "31222333");
        createShift(guardId, serviceId, "2026-10-12T08:00", "2026-10-12T20:00");
        createShift(guardId, serviceId, "2026-10-13T08:00", "2026-10-13T16:00");

        mockMvc.perform(get("/api/reports/monthly-hours").param("month", "2026-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHours").value(20.0))
                .andExpect(jsonPath("$.guards", hasSize(2)))
                .andExpect(jsonPath("$.guards[0].lastName").value("Gómez"))
                .andExpect(jsonPath("$.guards[0].totalHours").value(0.0))
                .andExpect(jsonPath("$.guards[1].lastName").value("Pérez"))
                .andExpect(jsonPath("$.guards[1].totalHours").value(20.0));
    }

    @Test
    void monthlyReportRejectsInvalidMonth() throws Exception {
        mockMvc.perform(get("/api/reports/monthly-hours").param("month", "octubre"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shiftsCanBeFilteredByServiceAndDay() throws Exception {
        createShift(guardId, serviceId, "2026-10-11T20:00", "2026-10-12T08:00");
        createShift(guardId, serviceId, "2026-10-12T20:00", "2026-10-13T08:00");
        createShift(guardId, serviceId, "2026-10-14T08:00", "2026-10-14T20:00");

        mockMvc.perform(get("/api/shift-assignments")
                        .param("serviceId", String.valueOf(serviceId))
                        .param("from", "2026-10-12")
                        .param("to", "2026-10-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void guardSearchMatchesNameAndDni() throws Exception {
        createGuard("María", "Gómez", "31222333");

        mockMvc.perform(get("/api/guards").param("search", "juan pérez"))
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/guards").param("search", "3122"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].firstName").value("María"));
    }

    @Test
    void invalidGuardReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/guards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"\",\"lastName\":\"X\",\"dni\":\"12a\",\"phone\":\"1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.firstName").exists())
                .andExpect(jsonPath("$.fields.dni").exists());
    }

    @Test
    void duplicatedDniIsAConflict() throws Exception {
        mockMvc.perform(post("/api/guards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(guardJson("Otro", "Guardia", "30111222")))
                .andExpect(status().isConflict());
    }

    @Test
    void requestsWithoutTokenAreUnauthorized() throws Exception {
        mockMvc.perform(get("/api/guards").with(anonymous()))
                .andExpect(status().isUnauthorized());
    }

    private long createGuard(String firstName, String lastName, String dni) throws Exception {
        String body = mockMvc.perform(post("/api/guards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(guardJson(firstName, lastName, dni)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return idOf(body);
    }

    private long createService(String name, String location) throws Exception {
        String body = mockMvc.perform(post("/api/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", name, "location", location))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return idOf(body);
    }

    private ResultActions createShift(long guard, long service, String start, String end) throws Exception {
        return mockMvc.perform(post("/api/shift-assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "guardId", guard,
                        "serviceId", service,
                        "startDateTime", start,
                        "endDateTime", end))));
    }

    private String guardJson(String firstName, String lastName, String dni) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "firstName", firstName, "lastName", lastName, "dni", dni, "phone", "1155550000"));
    }

    private long idOf(String json) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        return node.get("id").asLong();
    }
}
