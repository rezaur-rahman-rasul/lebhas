# Smoke Test Guide

Smoke tests validate internal readiness without calling real AI or payment providers.

Runs are protected by a Redis lock to avoid duplicate execution. Results are persisted in `platform.smoke_test_runs` and exposed through master APIs.
