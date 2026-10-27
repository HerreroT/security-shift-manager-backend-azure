package com.security.shiftmanager;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Uses real logins and JWTs: the admin is the one created by DataSeeder. */
@SpringBootTest(properties = "app.seed-demo-data=false")
@AutoConfigureMockMvc
@Transactional
class RoleAccessTest {

    private static final String SUPERVISOR_EMAIL = "supervisor.test@shiftmanager.com";
    private static final String SUPERVISOR_PASSWORD = "supervisor-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = login("admin@shiftmanager.com", "admin123", "ADMIN");
    }

    @Test
    void adminCanCreateSupervisorsAndSupervisorIsReadOnly() throws Exception {
        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "supervisor.test",
                                "email", SUPERVISOR_EMAIL,
                                "password", SUPERVISOR_PASSWORD,
                                "role", "SUPERVISOR"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("SUPERVISOR"));

        String supervisorToken = login(SUPERVISOR_EMAIL, SUPERVISOR_PASSWORD, "SUPERVISOR");

        mockMvc.perform(get("/api/guards").header("Authorization", bearer(supervisorToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/reports/monthly-hours").header("Authorization", bearer(supervisorToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/guards")
                        .header("Authorization", bearer(supervisorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "firstName", "Juan", "lastName", "Pérez", "dni", "30111222", "phone", "1155550000"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users").header("Authorization", bearer(supervisorToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/demo/slow-process").header("Authorization", bearer(supervisorToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanChangeData() throws Exception {
        mockMvc.perform(post("/api/services")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Barrio Belgrano", "location", "Av. Cabildo 2000"))))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/users").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
    }

    @Test
    void duplicatedUserEmailIsAConflict() throws Exception {
        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "otro",
                                "email", "admin@shiftmanager.com",
                                "password", "password123",
                                "role", "SUPERVISOR"))))
                .andExpect(status().isConflict());
    }

    private String login(String email, String password, String expectedRole) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value(expectedRole))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
