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
        Map<String, Object> brand = loadRow("platform.brands", request.brandId());
        Map<String, Object> product = request.productServiceId() == null ? Map.of() : loadRow("platform.product_services", request.productServiceId());
        if (product.isEmpty() && request.productServiceId() != null) {
            product = loadRow("platform.products", request.productServiceId());
        }
        Map<String, Object> campaign = request.campaignId() == null ? Map.of() : loadRow("platform.project_campaigns", request.campaignId());
        if (campaign.isEmpty() && request.campaignId() != null) {
            campaign = loadRow("platform.projects", request.campaignId());
        }
        if (campaign.isEmpty() && request.campaignId() != null) {
            campaign = loadRow("platform.campaigns", request.campaignId());
        }

        String brandName = string(brand.get("name"));
        String productName = firstNonBlank(string(product.get("name")), request.productDescription());
        String campaignName = string(campaign.get("name"));
        String language = firstNonBlank(
                request.language(),
                string(campaign.get("language")),
                string(campaign.get("languagePreference")),
                string(brand.get("languagePreference")));
        String platform = firstNonBlank(
                name(request.platform()),
                string(campaign.get("targetPlatform")));
        String preferredCta = request.includeCta() != null && !request.includeCta()
                ? null
                : firstNonBlank(
                        request.cta(),
                        string(campaign.get("cta")),
                        string(campaign.get("preferredCta")),
                        string(brand.get("preferredCta")));
        String audience = firstNonBlank(
                request.targetAudience(),
                string(campaign.get("targetAudience")),
                string(product.get("targetAudience")),
                string(brand.get("targetAudience")));
        String campaignObjective = firstNonBlank(
                request.campaignObjective(),
                string(campaign.get("campaignObjective")),
                string(campaign.get("description")),
                string(product.get("description")));
        String sellingPoints = firstNonBlank(
                string(product.get("sellingPoints")),
                string(product.get("benefits")),
                string(product.get("positioning")),
                string(product.get("description")));
        List<String> colors = new ArrayList<>();
        addIfPresent(colors, string(brand.get("primaryColor")));
        addIfPresent(colors, string(brand.get("secondaryColor")));

        Map<String, Object> creativeRequest = new LinkedHashMap<>();
        creativeRequest.put("campaignIdea", request.campaignIdea());
        creativeRequest.put("headline", request.headline());
        creativeRequest.put("subheadline", request.subheadline());
        creativeRequest.put("offerText", request.offerText());
        creativeRequest.put("cta", request.cta());
        creativeRequest.put("includeCta", request.includeCta());
        creativeRequest.put("platform", name(request.platform()));
        creativeRequest.put("creativeType", name(request.creativeType()));
        creativeRequest.put("tone", name(request.tone()));
        creativeRequest.put("quality", name(request.modelQuality()));

        return new CreativeContext(
                brandName,
                productName,
                campaignName,
                Map.copyOf(removeEmpty(brand)),
                Map.copyOf(removeEmpty(product)),
                Map.copyOf(removeEmpty(campaign)),
                Map.copyOf(removeEmpty(creativeRequest)),
                language,
                platform,
                name(request.creativeType()),
                name(request.tone()),
                firstNonBlank(name(request.quality()), name(request.modelQuality())),
                List.copyOf(colors),
                string(brand.get("logo")),
                string(brand.get("slogan")),
                audience,
                campaignObjective,
                sellingPoints,
                preferredCta);
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

    public void markJobStatus(UUID jobId, String status) {
        jdbc.update("""
                UPDATE platform.creative_generation_jobs
                SET status = ?, completed_at = CASE WHEN ? IN ('READY', 'COMPLETED', 'FAILED', 'CANCELLED') THEN NOW() ELSE completed_at END
                WHERE id = ?
                """, status, status, jobId);
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

    public void completeJob(
            UUID jobId,
            UUID generatedAssetId,
            String r2ObjectKey,
            String previewUrl,
            String downloadUrl,
            BigDecimal creditUsed
    ) {
        jdbc.update("""
                UPDATE platform.creative_generation_jobs
                SET status = 'READY',
                    final_output_asset_id = ?,
                    file_url = ?,
                    r2_object_key = ?,
                    preview_url = ?,
                    download_url = ?,
                    actual_credit_used = ?,
                    completed_at = NOW()
                WHERE id = ?
                """, generatedAssetId.toString(), previewUrl, r2ObjectKey, previewUrl, downloadUrl, creditUsed, jobId);
    }

    public void saveGeneratedVersion(
            UUID promptRequestId,
            UUID jobId,
            AiCreativeGenerateRequest request,
            String promptTitle,
            UUID generatedAssetId,
            String r2ObjectKey,
            String previewUrl,
            String downloadUrl,
            BigDecimal creditUsed,
            Integer width,
            Integer height,
            Long fileSize,
            String mimeType,
            String model,
            String metadata
    ) {
        jdbc.update("""
                INSERT INTO platform.generated_versions (
                    id, workspace_id, creative_request_id, project_campaign_id, version_number, version_name,
                    prompt_request_id, generation_job_id, brand_id, product_service_id, campaign_id,
                    prompt_title, file_url, r2_object_key, credit_used,
                    generated_asset_id, asset_id, preview_url, download_url, storage_key, file_size, mime_type,
                    width, height, generation_provider, generation_model, generated_by_provider, generated_by_model,
                    generation_metadata, generation_status, approval_status, status,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, 1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, 'OPENAI', ?, 'OPENAI', ?,
                    CAST(? AS jsonb), 'READY', 'NOT_SUBMITTED', 'ACTIVE',
                    NOW(), NOW())
                """,
                UUID.randomUUID(), request.workspaceId(), null, request.campaignId(), promptTitle,
                promptRequestId, jobId, request.brandId(), request.productServiceId(), request.campaignId(),
                promptTitle, previewUrl, r2ObjectKey, creditUsed,
                generatedAssetId, generatedAssetId, previewUrl, downloadUrl, r2ObjectKey, fileSize, mimeType,
                width, height, model, model, metadata);
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

    private Map<String, Object> loadRow(String table, UUID id) {
        if (id == null) {
            return Map.of();
        }
        try {
            return normalizeRow(jdbc.queryForMap("SELECT * FROM " + table + " WHERE id = ? AND is_deleted = FALSE", id));
        } catch (EmptyResultDataAccessException | org.springframework.jdbc.BadSqlGrammarException ignored) {
            return Map.of();
        }
    }

    private Map<String, Object> normalizeRow(Map<String, Object> row) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        row.forEach((key, value) -> {
            Object clean = cleanValue(value);
            if (clean != null && !(clean instanceof String text && text.isBlank())) {
                normalized.put(toCamelCase(key), clean);
            }
        });
        return normalized;
    }

    private Object cleanValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant().toString();
        }
        if (value instanceof java.time.temporal.TemporalAccessor) {
            return String.valueOf(value);
        }
        if (value instanceof UUID uuid) {
            return uuid.toString();
        }
        if (value instanceof String text) {
            return text.trim().isEmpty() ? null : text.trim();
        }
        return value;
    }

    private String toCamelCase(String key) {
        if (key == null || !key.contains("_")) {
            return key;
        }
        StringBuilder builder = new StringBuilder();
        boolean upper = false;
        for (char item : key.toCharArray()) {
            if (item == '_') {
                upper = true;
                continue;
            }
            builder.append(upper ? Character.toUpperCase(item) : Character.toLowerCase(item));
            upper = false;
        }
        return builder.toString();
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
        boolean banglaTypographyRequired = isBanglaLanguage(request.language())
                || containsBangla(request.headline())
                || containsBangla(request.subheadline())
                || containsBangla(request.offerText())
                || containsBangla(request.cta());
        rules.put("banglaTypographyRequired", banglaTypographyRequired);
        if (banglaTypographyRequired) {
            rules.put("doNotTrustAiGeneratedBanglaText", true);
            rules.put("finalBanglaTextRenderedByBackend", true);
            rules.put("unicodeNormalization", "NFC");
            rules.put("requiresConjunctIntegrity", true);
            rules.put("requiresOpenTypeBanglaFont", true);
            rules.put("textSafeEmptyRegionsOnlyInImagePrompt", true);
            rules.put("minimumCtaReadable", true);
        }
        return rules;
    }

    private Map<String, Object> variables(AiCreativeGenerateRequest request, CreativeContext context, String size, CreativeQuality quality, OutputFormat format, String background) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("brandName", context.brandName());
        variables.put("productServiceName", context.productServiceName());
        variables.put("campaignName", context.campaignName());
        variables.put("brand", context.brand());
        variables.put("productService", context.productService());
        variables.put("campaign", context.campaign());
        variables.put("creativeRequest", context.creativeRequest());
        variables.put("inheritedLanguage", context.language());
        variables.put("inheritedPlatform", context.platform());
        variables.put("inheritedCreativeType", context.creativeType());
        variables.put("inheritedTone", context.tone());
        variables.put("inheritedQuality", context.quality());
        variables.put("brandColors", context.colors());
        variables.put("logo", context.logo());
        variables.put("slogan", context.slogan());
        variables.put("audience", context.audience());
        variables.put("inheritedCampaignObjective", context.campaignObjective());
        variables.put("sellingPoints", context.sellingPoints());
        variables.put("preferredCTA", context.preferredCTA());
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

    private Map<String, Object> removeEmpty(Map<String, Object> source) {
        Map<String, Object> cleaned = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (value != null && (!(value instanceof String text) || !text.isBlank())) {
                cleaned.put(key, value);
            }
        });
        return cleaned;
    }

    private void addIfPresent(List<String> values, String value) {
        if (value != null && !value.isBlank()) {
            values.add(value.trim());
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private Map<String, Object> imageInputs(AiCreativeGenerateRequest request) {
        Map<String, Object> imageInputs = new LinkedHashMap<>();
        imageInputs.put("existingAssetId", request.existingAssetId());
        imageInputs.put("hasExistingAsset", request.existingAssetId() != null);
        imageInputs.put("logoAssetId", request.logoAssetId());
        imageInputs.put("hasLogoAsset", request.logoAssetId() != null);
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
            case "TEXT_OVERLAY" -> "Rendering real Bangla typography";
            case "TYPOGRAPHY_VALIDATION" -> "Validating Bangla typography";
            case "R2_UPLOAD" -> "Saving creative";
            default -> layerKey.replace('_', ' ').toLowerCase();
        };
    }

    private boolean isBanglaLanguage(String language) {
        if (language == null) {
            return false;
        }
        String normalized = language.trim().toLowerCase();
        return normalized.equals("bn")
                || normalized.equals("bangla")
                || normalized.equals("bengali")
                || normalized.contains("বাংলা")
                || normalized.contains("bangla")
                || normalized.contains("bengali");
    }

    private boolean containsBangla(String value) {
        return value != null && value.codePoints().anyMatch(codePoint -> codePoint >= 0x0980 && codePoint <= 0x09FF);
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

    public record CreativeContext(
            String brandName,
            String productServiceName,
            String campaignName,
            Map<String, Object> brand,
            Map<String, Object> productService,
            Map<String, Object> campaign,
            Map<String, Object> creativeRequest,
            String language,
            String platform,
            String creativeType,
            String tone,
            String quality,
            List<String> colors,
            String logo,
            String slogan,
            String audience,
            String campaignObjective,
            String sellingPoints,
            String preferredCTA
    ) {
    }
}
