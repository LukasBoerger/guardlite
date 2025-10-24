package com.guardlite.demo;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.guardlite.demo.repositories.CheckResultRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Startet den vollen Spring Context inkl. Security, JPA, Flyway.
 * Testet: register -> login -> website anlegen -> check anlegen -> manuell run -> Ergebnis in DB.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(statements = {
        "DELETE FROM check_results",
        "DELETE FROM checks",
        "DELETE FROM websites",
        "DELETE FROM users"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CheckFlowIT {

    static MockWebServer server;
    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper om;
    @Autowired
    CheckResultRepository results;
    private String token;

    @BeforeAll
    static void startServer() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterAll
    static void stopServer() throws IOException {
        server.shutdown();
    }

    private String json(Object o) throws Exception {
        return om.writeValueAsString(o);
    }

    @BeforeEach
    void registerAndLogin() throws Exception {
        // 1) register
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", "admin@example.com",
                                "password", "ChangeMe123!",
                                "role", "ADMIN"
                        ))))
                .andExpect(status().isOk());

        // 2) login
        var loginRes = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", "admin@example.com",
                                "password", "ChangeMe123!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        token = om.readTree(loginRes.getResponse().getContentAsByteArray()).get("token").asText();
        assertThat(token).contains(".");
    }

    @Test
    void full_flow_green_on_200() throws Exception {
        // Mock Zielseite liefert 200
        server.enqueue(new MockResponse().setResponseCode(200));

        // 3) website anlegen
        var websiteRes = mvc.perform(post("/api/websites")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "url", server.url("/ok").toString(),
                                "cms", "WordPress",
                                "active", true
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        var websiteId = UUID.fromString(
                om.readTree(websiteRes.getResponse().getContentAsByteArray()).get("id").asText()
        );

        // 4) check anlegen (alle 30s)
        var checkRes = mvc.perform(post("/api/websites/{id}/checks", websiteId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "type", "HTTP_UP",
                                "cadenceCron", "*/30 * * * * *",
                                "enabled", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.websiteId").value(websiteId.toString()))
                .andReturn();

        var checkId = UUID.fromString(
                om.readTree(checkRes.getResponse().getContentAsByteArray()).get("id").asText()
        );

        // 5) manuellen Run triggern
        mvc.perform(post("/api/checks/{id}/run", checkId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted());

        // 6) Ergebnis prüfen (direkt via Repository)
        var all = results.findAllByCheck_IdOrderByRunAtDesc(checkId);
        assertThat(all).isNotEmpty();
        assertThat(all.get(0).getStatus()).isEqualTo("GREEN");
        assertThat(all.get(0).getPayloadJson()).contains("\"httpStatus\":200");
    }

    @Test
    void create_check_rejects_bad_cron() throws Exception {
        // website
        var wRes = mvc.perform(post("/api/websites")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("url", "https://example.com", "cms", "WP", "active", true))))
                .andExpect(status().isOk())
                .andReturn();
        var websiteId = UUID.fromString(om.readTree(wRes.getResponse().getContentAsByteArray()).get("id").asText());

        // ungültiger Cron -> 400
        mvc.perform(post("/api/websites/{id}/checks", websiteId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("type", "HTTP_UP", "cadenceCron", "bad-cron", "enabled", true))))
                .andExpect(status().isBadRequest());
    }
}
