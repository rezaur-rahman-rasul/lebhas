package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class WorkspaceRequestResolver {

    private final CurrentUserContext currentUserContext;

    public WorkspaceRequestResolver(CurrentUserContext currentUserContext) {
        this.currentUserContext = currentUserContext;
    }

    public UUID requireWorkspaceId() {
        return currentUserContext.requireWorkspaceId();
    }
}
