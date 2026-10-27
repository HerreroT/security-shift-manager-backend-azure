package com.security.shiftmanager;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** The slow process used to demonstrate the graceful shutdown is ADMIN only and bounded. */
@SpringBootTest(properties = "app.seed-demo-data=false")
@AutoConfigureMockMvc
class DemoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void slowProcessRequiresToken() throws Exception {
        mockMvc.perform(get("/api/admin/demo/slow-process").param("seconds", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminRunsTheSlowProcess() throws Exception {
        String token = login("admin@shiftmanager.com", "admin123");
        mockMvc.perform(get("/api/admin/demo/slow-process").param("seconds", "1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seconds").value(1))
                .andExpect(jsonPath("$.finishedAt").exists());
    }

    @Test
    void secondsOutOfRangeIsABadRequest() throws Exception {
        String token = login("admin@shiftmanager.com", "admin123");
        mockMvc.perform(get("/api/admin/demo/slow-process").param("seconds", "0")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/demo/slow-process").param("seconds", "31")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }
}
