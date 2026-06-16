UPDATE platform.credit_value_policies
SET average_provider_cost_per_creative_usd = 0.190000,
    updated_at = NOW()
WHERE active = TRUE
  AND average_provider_cost_per_creative_usd = 0.150000;
