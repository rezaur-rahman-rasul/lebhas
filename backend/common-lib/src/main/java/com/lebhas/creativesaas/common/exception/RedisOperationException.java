package com.lebhas.creativesaas.common.exception;

public class RedisOperationException extends BusinessException {

    public RedisOperationException(String message) {
        super(ErrorCode.REDIS_OPERATION_FAILED, message);
    }
}
