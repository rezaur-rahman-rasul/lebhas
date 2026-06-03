package com.lebhas.creativesaas.credit.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Entity
@Table(name = "credit_wallets", schema = "platform")
public class CreditWalletEntity extends TenantAwareEntity {

    private static final BigDecimal DEFAULT_STARTER_CREDITS = new BigDecimal("25.0000");

    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(name = "reserved_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal reservedBalance;

    protected CreditWalletEntity() {
    }

    public static CreditWalletEntity initialize(UUID workspaceId) {
        CreditWalletEntity entity = new CreditWalletEntity();
        entity.assignWorkspace(workspaceId);
        entity.balance = DEFAULT_STARTER_CREDITS.setScale(4, RoundingMode.HALF_UP);
        entity.reservedBalance = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        return entity;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public BigDecimal getReservedBalance() {
        return reservedBalance;
    }

    public BigDecimal getAvailableBalance() {
        return balance.subtract(reservedBalance);
    }

    public void addBalance(BigDecimal amount) {
        this.balance = normalize(balance.add(normalize(amount)));
    }

    public void adjustBalance(BigDecimal amount) {
        BigDecimal adjusted = normalize(balance.add(normalize(amount)));
        if (adjusted.signum() < 0) {
            throw new BusinessException(ErrorCode.CREDIT_BALANCE_INSUFFICIENT, "Credit adjustment cannot make balance negative");
        }
        this.balance = adjusted;
    }

    public void reserve(BigDecimal amount) {
        BigDecimal normalizedAmount = normalize(amount);
        if (getAvailableBalance().compareTo(normalizedAmount) < 0) {
            throw new BusinessException(ErrorCode.CREDIT_BALANCE_INSUFFICIENT);
        }
        this.reservedBalance = normalize(reservedBalance.add(normalizedAmount));
    }

    public void finalizeReservation(BigDecimal amount) {
        BigDecimal normalizedAmount = normalize(amount);
        if (reservedBalance.compareTo(normalizedAmount) < 0) {
            throw new BusinessException(ErrorCode.CREDIT_RESERVE_INVALID, "Reserved balance cannot be negative");
        }
        this.reservedBalance = normalize(reservedBalance.subtract(normalizedAmount));
        this.balance = normalize(balance.subtract(normalizedAmount));
    }

    public void refundReservation(BigDecimal amount) {
        BigDecimal normalizedAmount = normalize(amount);
        if (reservedBalance.compareTo(normalizedAmount) < 0) {
            throw new BusinessException(ErrorCode.CREDIT_RESERVE_INVALID, "Reserved balance cannot be negative");
        }
        this.reservedBalance = normalize(reservedBalance.subtract(normalizedAmount));
    }

    private BigDecimal normalize(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return amount.setScale(4, RoundingMode.HALF_UP);
    }
}
