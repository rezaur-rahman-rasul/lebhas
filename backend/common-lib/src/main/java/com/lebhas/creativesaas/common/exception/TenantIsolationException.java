package com.lebhas.creativesaas.common.exception;

public class TenantIsolationException extends BusinessException {

    public TenantIsolationException(String message) {
        super(ErrorCode.WORKSPACE_ACCESS_DENIED, message);
    }
}
