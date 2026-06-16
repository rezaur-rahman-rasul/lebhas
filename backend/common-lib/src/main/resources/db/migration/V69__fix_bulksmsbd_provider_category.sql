UPDATE platform.ai_tool_providers
SET provider_type = 'SMS',
    category = 'SMS',
    supported_layers = CASE
        WHEN supported_layers IS NULL OR jsonb_array_length(supported_layers) = 0
            THEN '["OTP", "NOTIFICATION_SMS"]'::jsonb
        ELSE supported_layers
    END,
    updated_at = now()
WHERE provider_code = 'BULKSMSBD'
  AND is_deleted = false;
