package com.lebhas.creativesaas.creative;

import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.sharing.domain.PublicShareLinkEntity;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class Day4ShareLinkApiMockMvcIntegrationTest extends AbstractDay4BackendIntegrationTest {

    @Test
    void shareLinkGenerationWorks() throws Exception {
        String accessToken = tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN);
        GeneratedVersionEntity generatedVersion = createShareableGeneratedVersion();

        var response = mockMvc.perform(post("/api/v1/share-links")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "generatedVersionId", generatedVersion.getId(),
                                "expiresAt", Instant.now().plusSeconds(3600).toString(),
                                "password", "launch-asset"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.generatedVersionId").value(generatedVersion.getId().toString()))
                .andExpect(jsonPath("$.data.accessCount").value(0))
                .andReturn();

        String token = textAt(response, "/data/token");
        assertThat(token).isNotBlank();
        assertThat(publicShareLinkRepository.findByTokenAndDeletedFalse(token)).isPresent();
    }

    @Test
    void expiredLinkBlocked() throws Exception {
        GeneratedVersionEntity generatedVersion = createShareableGeneratedVersion();
        PublicShareLinkEntity expiredLink = publicShareLinkRepository.save(PublicShareLinkEntity.create(
                workspaceOne.getId(),
                generatedVersion.getId(),
                "expired-" + UUID.randomUUID(),
                Instant.now().minusSeconds(60),
                false,
                adminUser.getId()));

        mockMvc.perform(get("/api/v1/share-links/{token}", expiredLink.getToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors[0].code").value("AUTH-401-02"));
    }

    @Test
    void shareTokenUniquenessWorks() throws Exception {
        String accessToken = tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN);
        GeneratedVersionEntity generatedVersion = createShareableGeneratedVersion();
        byte[] body = objectMapper.writeValueAsBytes(Map.of(
                "generatedVersionId", generatedVersion.getId(),
                "expiresAt", Instant.now().plusSeconds(7200).toString()));

        var first = mockMvc.perform(post("/api/v1/share-links")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        var second = mockMvc.perform(post("/api/v1/share-links")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        String firstToken = textAt(first, "/data/token");
        String secondToken = textAt(second, "/data/token");

        assertThat(firstToken).isNotBlank();
        assertThat(secondToken).isNotBlank();
        assertThat(firstToken).isNotEqualTo(secondToken);
        assertThat(publicShareLinkRepository.findByTokenAndDeletedFalse(firstToken)).isPresent();
        assertThat(publicShareLinkRepository.findByTokenAndDeletedFalse(secondToken)).isPresent();
    }
}
