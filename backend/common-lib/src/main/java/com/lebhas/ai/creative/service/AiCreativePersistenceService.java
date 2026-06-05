package com.lebhas.ai.creative.service;

import com.lebhas.ai.creative.dto.AiCreativeGenerateRequest;
import com.lebhas.ai.creative.enums.CreativeQuality;
import com.lebhas.ai.creative.enums.GenerationMode;
import com.lebhas.ai.creative.enums.OutputFormat;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AiCreativePersistenceService {

    private final JdbcTemplate jdbc;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public AiCreativePersistenceService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
    }

    public CreativeContext context(AiCreativeGenerateRequest request) {
        String brandName = lookupName("platform.brands", request.brandId());
        String productName = request.productServiceId() == null ? null : lookupName("platform.product_services", request.productServiceId());
        if (productName == null && request.productServiceId() != null) {
            productName = lookupName("platform.products", request.productServiceId());
        }
        String campaignName = request.campaignId() == null ? null : lookupName("platform.projects", request.campaignId());
        if (campaignName == null && request.campaignId() != null) {
            campaignName = lookupName("platform.campaigns", request.campaignId());
        }
        return new CreativeContext(brandName, productName, campaignName);
    }

    public UUID createPromptRequest(
            AiCreativeGenerateRequest request,
            CreativeContext context,
            String promptTitle,
            GenerationMode mode,
            String size,
            CreativeQuality quality,
            OutputFormat outputFormat,
            String background,
            String finalPrompt,
            Map<String, Object> executionPlan
    ) {
        UUID id = UUID.randomUUID();
        String aspectRatio = switch (size) {
            case "1024x1536" -> "2:3";
            case "1536x1024" -> "3:2";
            default -> "1:1";
        };
        Map<String, Object> fixedRules = fixedRules(request, aspectRatio);
        Map<String, Object> variables = variables(request, context, size, quality, outputFormat, background);
        Map<String, Object> imageInputs = imageInputs(request);
        jdbc.update("""
                INSERT INTO platform.creative_prompt_requests (
                    id, workspace_id, brand_id, product_service_id, campaign_id, prompt_title,
                    platform, creative_type, aspect_ratio, size, language, tone, model_quality,
                    headline, subheadline, offer_text, cta_text, campaign_idea, campaign_objective,
                    target_audience, product_description, generation_mode, fixed_rules_json,
                    variable_inputs_json, image_inputs_json, final_prompt, status, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), CAST(? AS jsonb), ?, 'REQUESTED', NOW(), NOW())
                """,
                id, request.workspaceId(), request.brandId(), request.productServiceId(), request.campaignId(), promptTitle,
                name(request.platform()), name(request.creativeType()), aspectRatio, size, request.language(), name(request.tone()), name(request.modelQuality()),
                request.headline(), request.subheadline(), request.offerText(), request.cta(), request.campaignIdea(), request.campaignObjective(),
                request.targetAudience(), request.productDescription(), storageMode(mode), json(fixedRules), json(variables), json(imageInputs), finalPrompt);
        return id;
    }

    public UUID createJob(UUID promptRequestId, AiCreativeGenerateRequest request, String promptTitle, GenerationMode mode, Map<String, Object> plan, String model) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO platform.creative_generation_jobs (
                    id, prompt_request_id, workspace_id, brand_id, product_service_id, campaign_id, prompt_title,
                    provider, model, generation_mode, execution_plan_json, estimated_credit_cost, status, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, 'OPENAI', ?, ?, CAST(? AS jsonb), ?, 'PROCESSING', NOW())
                """,
                id, promptRequestId, request.workspaceId(), request.brandId(), request.productServiceId(), request.campaignId(),
                promptTitle, model, storageMode(mode), json(plan), BigDecimal.ONE);
        return id;
    }

    @SuppressWarnings("unchecked")
    public void createLayers(UUID jobId, GenerationMode mode, String model, Map<String, Object> plan) {
        Object rawLayers = plan.get("layers");
        if (!(rawLayers instanceof List<?> layers)) {
            return;
        }
        for (Object raw : layers) {
            if (!(raw instanceof Map<?, ?> layer)) {
                continue;
            }
            jdbc.update("""
                    INSERT INTO platform.creative_generation_job_layers (
                        id, job_id, sequence_no, layer_key, layer_type, provider, model, status,
                        input_payload_json, estimated_cost
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, 'PLANNED', CAST(? AS jsonb), ?)
                    """,
                    UUID.randomUUID(), jobId, number(layer.get("sequence")), string(layer.get("layerKey")),
                    string(layer.get("layerType")), string(layer.get("provider")), string(layer.get("model")),
                    json(Map.of("generationMode", storageMode(mode))), decimal(layer.get("estimatedCost")));
        }
    }

    public void markLayerProcessing(UUID jobId, String layerKey) {
        jdbc.update("""
                UPDATE platform.creative_generation_job_layers
                SET status = 'PROCESSING', started_at = COALESCE(started_at, NOW())
                WHERE job_id = ? AND layer_key = ?
                """, jobId, layerKey);
    }

    public void markLayerCompleted(UUID jobId, String layerKey, Map<String, Object> output) {
        jdbc.update("""
                UPDATE platform.creative_generation_job_layers
                SET status = 'COMPLETED', completed_at = NOW(), output_payload_json = CAST(? AS jsonb)
                WHERE job_id = ? AND layer_key = ?
                """, json(output), jobId, layerKey);
    }

    public void completePromptRequest(UUID promptRequestId, String finalPrompt) {
        jdbc.update("UPDATE platform.creative_prompt_requests SET status = 'COMPLETED', final_prompt = ?, updated_at = NOW() WHERE id = ?",
                finalPrompt, promptRequestId);
    }

    public void completeJob(UUID jobId, String r2ObjectKey, String fileUrl, BigDecimal creditUsed) {
        jdbc.update("""
                UPDATE platform.creative_generation_jobs
                SET status = 'COMPLETED', final_output_asset_id = ?, file_url = ?, actual_credit_used = ?, completed_at = NOW()
                WHERE id = ?
                """, r2ObjectKey, fileUrl, creditUsed, jobId);
    }

    public void saveGeneratedVersion(UUID promptRequestId, UUID jobId, AiCreativeGenerateRequest request, String promptTitle, String r2ObjectKey, String fileUrl, BigDecimal creditUsed) {
        jdbc.update("""
                INSERT INTO platform.generated_versions (
                    id, workspace_id, creative_request_id, project_campaign_id, version_number, version_name,
                    prompt_request_id, generation_job_id, brand_id, product_service_id, campaign_id,
                    prompt_title, file_url, r2_object_key, credit_used, generation_status, approval_status, status,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, 1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'COMPLETED', 'NOT_SUBMITTED', 'ACTIVE', NOW(), NOW())
                """,
                UUID.randomUUID(), request.workspaceId(), promptRequestId, request.campaignId(), promptTitle,
                promptRequestId, jobId, request.brandId(), request.productServiceId(), request.campaignId(),
                promptTitle, fileUrl, r2ObjectKey, creditUsed);
    }

    public void failJob(UUID jobId, String errorMessage) {
        jdbc.update("UPDATE platform.creative_generation_jobs SET status = 'FAILED', error_message = ?, completed_at = NOW() WHERE id = ?",
                errorMessage, jobId);
        jdbc.update("""
                UPDATE platform.creative_generation_job_layers
                SET status = 'FAILED', error_message = ?, completed_at = NOW()
                WHERE job_id = ? AND status = 'PROCESSING'
                """, errorMessage, jobId);
    }

    public void failPromptRequest(UUID promptRequestId, String errorMessage) {
        jdbc.update("UPDATE platform.creative_prompt_requests SET status = 'FAILED', updated_at = NOW() WHERE id = ?", promptRequestId);
    }

    public Map<String, Object> progress(UUID jobId) {
        Map<String, Object> job = jdbc.queryForMap("""
                SELECT id, status, generation_mode, prompt_title, provider, model
                FROM platform.creative_generation_jobs
                WHERE id = ?
                """, jobId);
        List<Map<String, Object>> layers = jdbc.query("""
                SELECT layer_key, layer_type, sequence_no, status, started_at, completed_at, error_message
                FROM platform.creative_generation_job_layers
                WHERE job_id = ?
                ORDER BY sequence_no
                """, (rs, rowNum) -> {
            Map<String, Object> layer = new LinkedHashMap<>();
            layer.put("layerKey", rs.getString("layer_key"));
            layer.put("label", label(rs.getString("layer_key")));
            layer.put("sequenceNo", rs.getInt("sequence_no"));
            layer.put("status", rs.getString("status"));
            layer.put("startedAt", instant(rs.getTimestamp("started_at")));
            layer.put("completedAt", instant(rs.getTimestamp("completed_at")));
            layer.put("errorMessage", rs.getString("error_message"));
            return layer;
        }, jobId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("creativeId", job.get("id"));
        response.put("status", job.get("status"));
        layers.stream().filter(layer -> "PROCESSING".equals(layer.get("status"))).findFirst().ifPresent(layer -> {
            response.put("currentLayerKey", layer.get("layerKey"));
            response.put("currentLayerLabel", layer.get("label"));
        });
        response.put("layers", layers);
        return response;
    }

    private String lookupName(String table, UUID id) {
        if (id == null) {
            return null;
        }
        try {
            return jdbc.queryForObject("SELECT name FROM " + table + " WHERE id = ? AND is_deleted = FALSE", String.class, id);
        } catch (EmptyResultDataAccessException | org.springframework.jdbc.BadSqlGrammarException ignored) {
            return null;
        }
    }

    private Map<String, Object> fixedRules(AiCreativeGenerateRequest request, String aspectRatio) {
        Map<String, Object> rules = new LinkedHashMap<>();
        rules.put("market", "Bangladesh");
        rules.put("campaignReady", true);
        rules.put("modernCleanPremiumLayout", true);
        rules.put("safeSpacing", true);
        rules.put("textMustBeReadable", true);
        rules.put("noThirdPartyLogos", true);
        rules.put("avoidIrrelevantText", true);
        rules.put("avoidProductDistortion", true);
        rules.put("noHumanModel", request.noHumanModel() == null || request.noHumanModel());
        rules.put("avoidClutteredBackground", true);
        rules.put("commercialLighting", true);
        rules.put("aspectRatio", aspectRatio);
        rules.put("language", request.language());
        rules.put("tone", name(request.tone()));
        return rules;
    }

    private Map<String, Object> variables(AiCreativeGenerateRequest request, CreativeContext context, String size, CreativeQuality quality, OutputFormat format, String background) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("brandName", context.brandName());
        variables.put("productServiceName", context.productServiceName());
        variables.put("campaignName", context.campaignName());
        variables.put("platform", name(request.platform()));
        variables.put("creativeType", name(request.creativeType()));
        variables.put("size", size);
        variables.put("language", request.language());
        variables.put("tone", name(request.tone()));
        variables.put("modelQuality", name(request.modelQuality()));
        variables.put("quality", quality.name());
        variables.put("outputFormat", format.name());
        variables.put("background", background);
        variables.put("headline", request.headline());
        variables.put("subheadline", request.subheadline());
        variables.put("offerText", request.offerText());
        variables.put("ctaText", request.cta());
        variables.put("campaignIdea", request.campaignIdea());
        variables.put("targetAudience", request.targetAudience());
        variables.put("productDescription", request.productDescription());
        variables.put("campaignObjective", request.campaignObjective());
        return variables;
    }

    private Map<String, Object> imageInputs(AiCreativeGenerateRequest request) {
        Map<String, Object> imageInputs = new LinkedHashMap<>();
        imageInputs.put("existingAssetId", request.existingAssetId());
        imageInputs.put("hasExistingAsset", request.existingAssetId() != null);
        return imageInputs;
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new IllegalArgumentException("Could not serialize AI creative metadata", exception);
        }
    }

    private String storageMode(GenerationMode mode) {
        return switch (mode) {
            case TEXT_TO_CREATIVE -> "TEXT_ONLY_CREATIVE";
            case PRODUCT_IMAGE_TO_CREATIVE -> "PRODUCT_IMAGE_CREATIVE";
            case MULTI_REFERENCE -> "MULTI_REFERENCE_CREATIVE";
            case BACKGROUND_REPLACE -> "BACKGROUND_REPLACEMENT";
            case TRANSPARENT_ASSET -> "TRANSPARENT_ASSET";
        };
    }

    private String label(String layerKey) {
        if (layerKey == null) {
            return "Processing";
        }
        return switch (layerKey) {
            case "REQUEST_ANALYSIS" -> "Understanding brand";
            case "PROMPT_GENERATION" -> "Creating ad layout";
            case "IMAGE_GENERATION" -> "Generating creative";
            case "IMAGE_EDIT" -> "Editing product image";
            case "IMAGE_DECODE" -> "Preparing output";
            case "R2_UPLOAD" -> "Saving creative";
            default -> layerKey.replace('_', ' ').toLowerCase();
        };
    }

    private String name(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private int number(Object value) {
        return value instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(value));
    }

    private BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    public record CreativeContext(String brandName, String productServiceName, String campaignName) {
    }
}
