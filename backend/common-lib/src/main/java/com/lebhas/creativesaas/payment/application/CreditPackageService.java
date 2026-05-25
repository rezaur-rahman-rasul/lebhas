package com.lebhas.creativesaas.payment.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import com.lebhas.creativesaas.payment.application.dto.CreditPackageCommand;
import com.lebhas.creativesaas.payment.application.dto.CreditPackageView;
import com.lebhas.creativesaas.payment.cache.CreditPackageCacheService;
import com.lebhas.creativesaas.payment.domain.CreditPackage;
import com.lebhas.creativesaas.payment.infrastructure.persistence.CreditPackageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class CreditPackageService {

    private final CurrentUserContext currentUserContext;
    private final CreditPackageRepository creditPackageRepository;
    private final CreditPackageMapper mapper;
    private final CreditPackageCacheService creditPackageCacheService;

    public CreditPackageService(
            CurrentUserContext currentUserContext,
            CreditPackageRepository creditPackageRepository,
            CreditPackageMapper mapper,
            CreditPackageCacheService creditPackageCacheService
    ) {
        this.currentUserContext = currentUserContext;
        this.creditPackageRepository = creditPackageRepository;
        this.mapper = mapper;
        this.creditPackageCacheService = creditPackageCacheService;
    }

    @Transactional
    public CreditPackageView createCreditPackage(CreditPackageCommand command) {
        requireMaster();
        validateUniqueCode(command.code(), null);
        CreditPackage creditPackage = CreditPackage.create(
                command.name(),
                command.code(),
                command.credits(),
                command.bonusCredits(),
                command.price(),
                command.currency(),
                command.active(),
                command.sortOrder()
        );
        CreditPackageView view = mapper.toView(creditPackageRepository.save(creditPackage));
        creditPackageCacheService.cacheCreditPackage(view);
        creditPackageCacheService.invalidateActiveCreditPackages();
        return view;
    }

    @Transactional
    public CreditPackageView updateCreditPackage(CreditPackageCommand command) {
        requireMaster();
        CreditPackage creditPackage = requireCreditPackage(command.creditPackageId());
        validateUniqueCode(command.code(), creditPackage.getId());
        creditPackage.update(
                command.name(),
                command.code(),
                command.credits(),
                command.bonusCredits(),
                command.price(),
                command.currency(),
                command.active(),
                command.sortOrder()
        );
        CreditPackageView view = mapper.toView(creditPackageRepository.save(creditPackage));
        creditPackageCacheService.cacheCreditPackage(view);
        creditPackageCacheService.invalidateActiveCreditPackages();
        return view;
    }

    @Transactional
    public CreditPackageView disableCreditPackage(UUID creditPackageId) {
        requireMaster();
        CreditPackage creditPackage = requireCreditPackage(creditPackageId);
        creditPackage.deactivate();
        CreditPackageView view = mapper.toView(creditPackageRepository.save(creditPackage));
        creditPackageCacheService.invalidateCreditPackage(creditPackage.getId());
        return view;
    }

    @Transactional(readOnly = true)
    public List<CreditPackageView> listCreditPackagesForMaster() {
        requireMaster();
        return mapper.toViews(creditPackageRepository.findAllByOrderBySortOrderAscNameAsc());
    }

    @Transactional(readOnly = true)
    public CreditPackageView getCreditPackageForMaster(UUID creditPackageId) {
        requireMaster();
        return creditPackageCacheService.getCreditPackage(creditPackageId)
                .orElseGet(() -> {
                    CreditPackageView view = mapper.toView(requireCreditPackage(creditPackageId));
                    creditPackageCacheService.cacheCreditPackage(view);
                    return view;
                });
    }

    @Transactional(readOnly = true)
    public List<CreditPackageView> listActiveCreditPackages() {
        return creditPackageCacheService.getActiveCreditPackages()
                .orElseGet(() -> {
                    List<CreditPackageView> packages = mapper.toViews(creditPackageRepository.findAllByActiveTrueOrderBySortOrderAscNameAsc());
                    creditPackageCacheService.cacheActiveCreditPackages(packages);
                    return packages;
                });
    }

    private CreditPackage requireCreditPackage(UUID creditPackageId) {
        if (creditPackageId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Credit package id is required");
        }
        return creditPackageRepository.findById(creditPackageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Credit package not found"));
    }

    private void validateUniqueCode(String code, UUID currentCreditPackageId) {
        String normalizedCode = normalizeCode(code);
        creditPackageRepository.findByCode(normalizedCode)
                .filter(existing -> !existing.getId().equals(currentCreditPackageId))
                .ifPresent(existing -> {
                    throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Credit package code is already in use");
                });
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Credit package code is required");
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private CurrentUser requireMaster() {
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        if (!currentUser.isMaster()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return currentUser;
    }
}
