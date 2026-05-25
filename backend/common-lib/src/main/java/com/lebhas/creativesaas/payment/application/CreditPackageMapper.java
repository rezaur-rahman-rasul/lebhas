package com.lebhas.creativesaas.payment.application;

import com.lebhas.creativesaas.payment.application.dto.CreditPackageView;
import com.lebhas.creativesaas.payment.domain.CreditPackage;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CreditPackageMapper {

    public CreditPackageView toView(CreditPackage creditPackage) {
        return new CreditPackageView(
                creditPackage.getId(),
                creditPackage.getName(),
                creditPackage.getCode(),
                creditPackage.getCredits(),
                creditPackage.getBonusCredits(),
                creditPackage.getCredits() + creditPackage.getBonusCredits(),
                creditPackage.getPrice(),
                creditPackage.getCurrency(),
                creditPackage.isActive(),
                creditPackage.getSortOrder(),
                creditPackage.getCreatedAt(),
                creditPackage.getUpdatedAt()
        );
    }

    public List<CreditPackageView> toViews(List<CreditPackage> creditPackages) {
        return creditPackages.stream().map(this::toView).toList();
    }
}
