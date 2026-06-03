UPDATE platform.ai_tool_providers
SET supported_layers = CASE provider_code
    WHEN 'OPENAI' THEN '["TEXT_GENERATION","IMAGE_GENERATION","PROMPT_ENHANCEMENT"]'::jsonb
    WHEN 'ANTHROPIC' THEN '["TEXT_GENERATION","PROMPT_ENHANCEMENT"]'::jsonb
    WHEN 'GEMINI' THEN '["TEXT_GENERATION","IMAGE_GENERATION","PROMPT_ENHANCEMENT"]'::jsonb
    WHEN 'STABILITY' THEN '["IMAGE_GENERATION"]'::jsonb
    WHEN 'BKASH' THEN '["PAYMENT_COLLECTION","WEBHOOK_CALLBACK"]'::jsonb
    WHEN 'NAGAD' THEN '["PAYMENT_COLLECTION","WEBHOOK_CALLBACK"]'::jsonb
    WHEN 'SSLCOMMERZ' THEN '["PAYMENT_COLLECTION","WEBHOOK_CALLBACK"]'::jsonb
    ELSE supported_layers
END
WHERE provider_code IN ('OPENAI', 'ANTHROPIC', 'GEMINI', 'STABILITY', 'BKASH', 'NAGAD', 'SSLCOMMERZ')
  AND is_deleted = FALSE;

INSERT INTO platform.creative_tools (
    id, tool_code, tool_name, tool_category, enabled, description, metadata
)
VALUES
    (gen_random_uuid(), 'CAMPAIGN_CREATIVE_GENERATOR', 'Campaign Creative Generator', 'PRODUCT_IMAGE_CREATIVE', TRUE, 'Generates campaign-ready creative assets.', '{"defaultCreditCost":0,"requiredCapabilities":["TEXT_GENERATION","IMAGE_GENERATION"],"availableToAdmin":true,"availableToCrew":true,"systemDefault":true}'::jsonb),
    (gen_random_uuid(), 'POST_GENERATOR', 'Post Generator', 'SOCIAL_POST', TRUE, 'Generates social post creative content.', '{"defaultCreditCost":0,"requiredCapabilities":["TEXT_GENERATION","IMAGE_GENERATION"],"availableToAdmin":true,"availableToCrew":true,"systemDefault":true}'::jsonb),
    (gen_random_uuid(), 'CAPTION_GENERATOR', 'Caption Generator', 'CAPTION', TRUE, 'Generates captions for social and campaign assets.', '{"defaultCreditCost":0,"requiredCapabilities":["TEXT_GENERATION"],"availableToAdmin":true,"availableToCrew":true,"systemDefault":true}'::jsonb),
    (gen_random_uuid(), 'AD_COPY_GENERATOR', 'Ad Copy Generator', 'ADS_COPY', TRUE, 'Generates ad copy variants.', '{"defaultCreditCost":0,"requiredCapabilities":["TEXT_GENERATION"],"availableToAdmin":true,"availableToCrew":true,"systemDefault":true}'::jsonb),
    (gen_random_uuid(), 'HASHTAG_GENERATOR', 'Hashtag Generator', 'HASHTAGS', TRUE, 'Generates hashtag suggestions.', '{"defaultCreditCost":0,"requiredCapabilities":["TEXT_GENERATION"],"availableToAdmin":true,"availableToCrew":true,"systemDefault":true}'::jsonb),
    (gen_random_uuid(), 'PRODUCT_VIDEO_GENERATOR', 'Product Video Generator', 'PRODUCT_VIDEO', TRUE, 'Generates product video creative content.', '{"defaultCreditCost":0,"requiredCapabilities":["VIDEO_GENERATION","TEXT_GENERATION"],"availableToAdmin":true,"availableToCrew":true,"systemDefault":true}'::jsonb),
    (gen_random_uuid(), 'VOICEOVER_GENERATOR', 'Voiceover Generator', 'VOICEOVER', TRUE, 'Generates voiceover scripts or audio-ready copy.', '{"defaultCreditCost":0,"requiredCapabilities":["AUDIO_GENERATION","TEXT_GENERATION"],"availableToAdmin":true,"availableToCrew":true,"systemDefault":true}'::jsonb)
ON CONFLICT DO NOTHING;

INSERT INTO platform.creative_pipelines (
    id, pipeline_code, pipeline_name, description, status, active, pipeline_version, metadata
)
VALUES (
    gen_random_uuid(),
    'MASTER_CREATIVE_GENERATION',
    'Master Creative Generation',
    'Default master-managed creative generation pipeline.',
    'ACTIVE',
    FALSE,
    1,
    '{"systemDefault":true}'::jsonb
)
ON CONFLICT DO NOTHING;

WITH pipeline AS (
    SELECT id
    FROM platform.creative_pipelines
    WHERE pipeline_code = 'MASTER_CREATIVE_GENERATION'
      AND is_deleted = FALSE
    LIMIT 1
)
INSERT INTO platform.creative_pipeline_layers (
    id, pipeline_id, layer_type, layer_code, layer_name, sort_order, enabled, required_layer, retryable, configuration
)
SELECT gen_random_uuid(), pipeline.id, seed.layer_type, seed.layer_code, seed.layer_name, seed.sort_order,
       TRUE, TRUE, TRUE, seed.configuration
FROM pipeline
CROSS JOIN (
    VALUES
        ('PROMPT_UNDERSTANDING', 'PROMPT_UNDERSTANDING', 'Prompt Understanding', 1, '{"timeoutMs":30000,"retryCount":1,"costWeight":1,"requiredCapability":"PROMPT_ENHANCEMENT"}'::jsonb),
        ('BRAND_CONTEXT', 'BRAND_CONTEXT', 'Brand Context', 2, '{"timeoutMs":30000,"retryCount":1,"costWeight":1,"requiredCapability":"TEXT_GENERATION"}'::jsonb),
        ('PRODUCT_CONTEXT', 'PRODUCT_CONTEXT', 'Product Context', 3, '{"timeoutMs":30000,"retryCount":1,"costWeight":1,"requiredCapability":"TEXT_GENERATION"}'::jsonb),
        ('ASSET_ANALYSIS', 'ASSET_ANALYSIS', 'Asset Analysis', 4, '{"timeoutMs":45000,"retryCount":1,"costWeight":1,"requiredCapability":"IMAGE_GENERATION"}'::jsonb),
        ('CREATIVE_GENERATION', 'CREATIVE_GENERATION', 'Creative Generation', 5, '{"timeoutMs":90000,"retryCount":2,"costWeight":2,"requiredCapability":"IMAGE_GENERATION"}'::jsonb),
        ('QUALITY_REVIEW', 'QUALITY_REVIEW', 'Quality Review', 6, '{"timeoutMs":30000,"retryCount":1,"costWeight":1,"requiredCapability":"TEXT_GENERATION"}'::jsonb),
        ('EXPORT_PREPARATION', 'EXPORT_PREPARATION', 'Export Preparation', 7, '{"timeoutMs":30000,"retryCount":1,"costWeight":1,"requiredCapability":"TEXT_GENERATION"}'::jsonb)
) AS seed(layer_type, layer_code, layer_name, sort_order, configuration)
ON CONFLICT DO NOTHING;

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.master_ai_operations_seed_contracts', '53')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
