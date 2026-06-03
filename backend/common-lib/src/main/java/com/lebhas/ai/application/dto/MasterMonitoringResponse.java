package com.lebhas.ai.application.dto;

import java.util.List;

public record MasterMonitoringResponse<S, T>(
        S summary,
        List<T> items
) {
    public static <S, T> MasterMonitoringResponse<S, T> of(S summary, List<T> items) {
        return new MasterMonitoringResponse<>(summary, items == null ? List.of() : List.copyOf(items));
    }
}
