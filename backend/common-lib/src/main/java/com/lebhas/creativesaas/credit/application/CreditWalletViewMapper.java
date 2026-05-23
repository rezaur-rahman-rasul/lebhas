package com.lebhas.creativesaas.credit.application;

import com.lebhas.creativesaas.credit.application.dto.CreditWalletView;
import com.lebhas.creativesaas.credit.domain.CreditWalletEntity;
import org.springframework.stereotype.Component;

@Component
public class CreditWalletViewMapper {

    public CreditWalletView toView(CreditWalletEntity entity) {
        return new CreditWalletView(
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getBalance(),
                entity.getReservedBalance(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
