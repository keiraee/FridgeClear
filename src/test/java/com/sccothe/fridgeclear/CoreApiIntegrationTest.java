package com.sccothe.fridgeclear;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sccothe.fridgeclear.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class CoreApiIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void authPantryRecommendationAndMealPlanSubmitFlow() throws Exception {
        String email = "integration-" + System.currentTimeMillis() + "@example.com";
        String password = "Passw0rd!";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","nickname":"集成测试"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email));

        mockMvc.perform(post("/api/v1/pantry-items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rawName":"西红柿","quantity":2,"unit":"个","expireDate":"2026-12-31"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rawName").value("西红柿"));

        mockMvc.perform(get("/api/v1/pantry-items")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(get("/api/v1/recommendations/recipes")
                        .header("Authorization", "Bearer " + token)
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recipes").isArray());

        MvcResult generateResult = mockMvc.perform(post("/api/v1/meal-plans/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"days":1,"peopleCount":2,"maxCookingMinutes":30,"mealTypes":["DINNER"]}
                                """))
                .andReturn();

        int generateStatus = generateResult.getResponse().getStatus();
        assertThat(generateStatus).isIn(202, 503);
        JsonNode generateBody = objectMapper.readTree(generateResult.getResponse().getContentAsString());
        if (generateStatus == 202) {
            long taskId = generateBody.path("data").path("taskId").asLong();
            assertThat(taskId).isPositive();
            mockMvc.perform(get("/api/v1/meal-plans/generate/tasks/" + taskId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.taskId").value(taskId));
        } else {
            assertThat(generateBody.path("code").asText()).isEqualTo("AI_SERVICE_UNAVAILABLE");
        }
    }

    @Test
    void telemetryAcceptsInvalidJwtAsAnonymous() throws Exception {
        mockMvc.perform(post("/api/v1/telemetry/access")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"test-client","pagePath":"/"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }
}
