package com.lebhas.creativesaas.payment.infrastructure.persistence;

import com.lebhas.creativesaas.payment.domain.CreditPackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CreditPackageRepository extends JpaRepository<CreditPackage, UUID> {

    Optional<CreditPackage> findByCode(String code);

    List<CreditPackage> findAllByActiveTrueOrderBySortOrderAscNameAsc();

    List<CreditPackage> findAllByOrderBySortOrderAscNameAsc();
}
