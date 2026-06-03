# Feature Toggle Guide

Feature toggles are Master-controlled and cached in Redis.

Supported toggles include AI generation, asset upload, prompt enhancement, text tools, image creative generation, approval workflow, public sharing, downloads, payments, registration, workspace creation, maintenance mode, and beta-only mode.

Toggle changes publish Kafka audit events and invalidate the Redis cache entry for the toggle key.
