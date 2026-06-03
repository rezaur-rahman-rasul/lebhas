package com.lebhas.creativesaas.operations.application;

import com.lebhas.creativesaas.messaging.kafka.BaseDomainEvent;
import com.lebhas.creativesaas.messaging.kafka.DomainEventPublisher;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.operations.application.dto.OperationsViews.MaintenanceStatusView;
import com.lebhas.creativesaas.operations.application.dto.OperationsViews.ToggleView;
import com.lebhas.creativesaas.operations.domain.SystemFeatureToggle;
import com.lebhas.creativesaas.operations.domain.SystemFeatureToggleKey;
import com.lebhas.creativesaas.operations.infrastructure.persistence.SystemFeatureToggleRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class SystemFeatureToggleService {
    private static final String CACHE_PREFIX = "ops:feature-toggle:";

    private final SystemFeatureToggleRepository repository;
    private final ObjectProvider<StringRedisTemplate> redisTemplate;
    private DomainEventPublisher domainEventPublisher;

    public SystemFeatureToggleService(SystemFeatureToggleRepository repository, ObjectProvider<StringRedisTemplate> redisTemplate) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
    }

    @Autowired(required = false)
    void setDomainEventPublisher(DomainEventPublisher domainEventPublisher) {
        this.domainEventPublisher = domainEventPublisher;
    }

    @Transactional(readOnly = true)
    public List<ToggleView> list() {
        Map<SystemFeatureToggleKey, SystemFeatureToggle> configured = new EnumMap<>(SystemFeatureToggleKey.class);
        repository.findAllByDeletedFalseOrderByToggleKeyAsc().forEach(toggle -> configured.put(toggle.getToggleKey(), toggle));
        return Arrays.stream(SystemFeatureToggleKey.values())
                .map(key -> toView(configured.get(key), key))
                .toList();
    }

    @Transactional
    public ToggleView update(SystemFeatureToggleKey key, boolean enabled, String reason) {
        SystemFeatureToggle toggle = repository.findByToggleKeyAndDeletedFalse(key)
                .orElseGet(() -> SystemFeatureToggle.create(key, defaultValue(key), null));
        toggle.update(enabled, reason);
        toggle = repository.save(toggle);
        evict(key);
        publish(KafkaTopicConstants.SYSTEM_FEATURE_TOGGLE_UPDATED, key.name(), Map.of("enabled", enabled));
        return toView(toggle, key);
    }

    @Transactional
    public MaintenanceStatusView setMaintenanceMode(boolean enabled, String reason) {
        update(SystemFeatureToggleKey.MAINTENANCE_MODE, enabled, reason);
        publish(enabled ? KafkaTopicConstants.MAINTENANCE_MODE_ENABLED : KafkaTopicConstants.MAINTENANCE_MODE_DISABLED,
                "maintenance", Map.of("enabled", enabled));
        return maintenanceStatus();
    }

    @Transactional(readOnly = true)
    public MaintenanceStatusView maintenanceStatus() {
        return new MaintenanceStatusView(isEnabled(SystemFeatureToggleKey.MAINTENANCE_MODE), isEnabled(SystemFeatureToggleKey.BETA_ONLY_MODE));
    }

    @Transactional(readOnly = true)
    public Map<SystemFeatureToggleKey, Boolean> snapshot() {
        Map<SystemFeatureToggleKey, Boolean> values = new EnumMap<>(SystemFeatureToggleKey.class);
        for (SystemFeatureToggleKey key : SystemFeatureToggleKey.values()) {
            values.put(key, isEnabled(key));
        }
        return values;
    }

    @Transactional(readOnly = true)
    public boolean isEnabled(SystemFeatureToggleKey key) {
        StringRedisTemplate redis = redisTemplate.getIfAvailable();
        String cacheKey = CACHE_PREFIX + key.name();
        if (redis != null) {
            String cached = redis.opsForValue().get(cacheKey);
            if (cached != null) {
                return Boolean.parseBoolean(cached);
            }
        }
        boolean enabled = repository.findByToggleKeyAndDeletedFalse(key).map(SystemFeatureToggle::isEnabled).orElse(defaultValue(key));
        if (redis != null) {
            redis.opsForValue().set(cacheKey, Boolean.toString(enabled), Duration.ofMinutes(10));
        }
        return enabled;
    }

    private void evict(SystemFeatureToggleKey key) {
        StringRedisTemplate redis = redisTemplate.getIfAvailable();
        if (redis != null) {
            redis.delete(CACHE_PREFIX + key.name());
        }
    }

    private ToggleView toView(SystemFeatureToggle toggle, SystemFeatureToggleKey key) {
        return new ToggleView(key, toggle == null ? defaultValue(key) : toggle.isEnabled(), toggle == null ? null : toggle.getReason(), toggle == null ? null : toggle.getUpdatedAt());
    }

    private boolean defaultValue(SystemFeatureToggleKey key) {
        return key != SystemFeatureToggleKey.MAINTENANCE_MODE && key != SystemFeatureToggleKey.BETA_ONLY_MODE;
    }

    private void publish(String topic, String aggregateId, Map<String, Object> attributes) {
        if (domainEventPublisher != null) {
            domainEventPublisher.publish(topic, new BaseDomainEvent(topic, null, java.util.UUID.nameUUIDFromBytes(aggregateId.getBytes()), Instant.now(), attributes));
        }
    }
}
