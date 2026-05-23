package com.lebhas.creativesaas.common.exception;

public class KafkaPublishingException extends BusinessException {

    public KafkaPublishingException(String message) {
        super(ErrorCode.KAFKA_PUBLISH_FAILED, message);
    }
}
