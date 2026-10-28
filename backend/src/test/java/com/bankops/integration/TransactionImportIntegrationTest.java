package com.bankops.integration;

import com.bankops.modules.auth.AuthResponse;
import com.bankops.modules.auth.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionImportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String jwtToken;

    @BeforeEach
    void setUp() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .username("analyst_david")
                .password("Password123!")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        var apiResp = objectMapper.readTree(responseBody);
        jwtToken = apiResp.get("data").get("token").asText();
    }

    @Test
    @DisplayName("Should successfully import CSV, trigger rule exceptions, and handle duplicates safely")
    void shouldImportCsvAndTriggerExceptions() throws Exception {
        String csvContent = "external_reference,account_reference,amount,currency,transaction_type,channel,status,merchant_or_destination,failure_reason,transaction_timestamp\n" +
                "TX-INT-101,ACT-991,1200.00,USD,PAYMENT,WEB,COMPLETED,AMAZON,,2026-10-25T14:00:00Z\n" +
                "TX-INT-102,ACT-992,85000.00,USD,WIRE_TRANSFER,BRANCH,COMPLETED,CORP_HQ,,2026-10-25T14:30:00Z\n" + // Triggers high value rule!
                "TX-INT-103,ACT-993,450.00,USD,PAYMENT,WEB,FAILED,SERVICES,GATEWAY_REJECTED,2026-10-25T15:00:00Z\n"; // Triggers failed status rule!

        MockMultipartFile file = new MockMultipartFile(
                "file", "test_import.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/transactions/import")
                        .file(file)
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Idempotency-Key", "IDEMP-TEST-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.successfullyImportedCount", is(3)))
                .andExpect(jsonPath("$.data.exceptionCasesTriggeredCount", greaterThanOrEqualTo(2)));

        // Replay same import with same Idempotency Key -> should return cached result without duplicate insertions
        mockMvc.perform(multipart("/api/v1/transactions/import")
                        .file(file)
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Idempotency-Key", "IDEMP-TEST-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successfullyImportedCount", is(3)));
    }
}
