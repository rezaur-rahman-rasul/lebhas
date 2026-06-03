package com.lebhas.creativesaas.texttool.application;

import com.lebhas.creativesaas.texttool.application.dto.CreativeTextToolHistoryView;
import com.lebhas.creativesaas.texttool.application.dto.CreativeTextToolOutputView;
import com.lebhas.creativesaas.texttool.domain.CreativeTextToolHistory;
import com.lebhas.creativesaas.texttool.domain.CreativeTextToolOutput;
import org.springframework.stereotype.Component;

@Component
public class CreativeTextToolMapper {

    public CreativeTextToolOutputView toView(CreativeTextToolOutput output) {
        return new CreativeTextToolOutputView(
                output.getId(),
                output.getWorkspaceId(),
                output.getProjectId(),
                output.getBrandId(),
                output.getProductServiceId(),
                output.getToolType(),
                output.getToolCode(),
                output.getQualityMode(),
                output.getPlatform(),
                output.getLanguage(),
                output.getTone(),
                output.getCampaignObjective(),
                output.getSourceIdea(),
                output.getCreditCost(),
                output.getCreditReservationId(),
                output.getOutputPayload(),
                output.getCreatedAt());
    }

    public CreativeTextToolHistoryView toView(CreativeTextToolHistory history) {
        return new CreativeTextToolHistoryView(
                history.getId(),
                history.getWorkspaceId(),
                history.getProjectId(),
                history.getTextToolOutputId(),
                history.getToolType(),
                history.getToolCode(),
                history.getStatus(),
                history.getCreditCost(),
                history.getFailureReason(),
                history.getRequestPayload(),
                history.getResponsePayload(),
                history.getCreatedAt());
    }
}
