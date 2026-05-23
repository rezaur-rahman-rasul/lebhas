package com.lebhas.creativesaas.prompt.application;

import com.lebhas.creativesaas.common.api.PagedResult;
import com.lebhas.creativesaas.prompt.application.dto.PromptHistoryFilter;
import com.lebhas.creativesaas.prompt.application.dto.PromptHistoryView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PromptHistoryQueryService {

    private final PromptHistoryService promptHistoryService;

    public PromptHistoryQueryService(PromptHistoryService promptHistoryService) {
        this.promptHistoryService = promptHistoryService;
    }

    @Transactional(readOnly = true)
    public PagedResult<PromptHistoryView> listHistory(PromptHistoryFilter filter) {
        return promptHistoryService.listHistory(filter);
    }

    @Transactional(readOnly = true)
    public PromptHistoryView getHistory(UUID workspaceId, UUID historyId) {
        return promptHistoryService.getHistory(workspaceId, null, historyId);
    }
}
