package com.tiktel.ttelgo.vendor.application;

import com.tiktel.ttelgo.common.domain.enums.BillingMode;
import com.tiktel.ttelgo.common.domain.enums.LedgerEntryStatus;
import com.tiktel.ttelgo.common.domain.enums.LedgerEntryType;
import com.tiktel.ttelgo.common.domain.enums.VendorStatus;
import com.tiktel.ttelgo.common.exception.BusinessException;
import com.tiktel.ttelgo.common.exception.ErrorCode;
import com.tiktel.ttelgo.common.exception.ResourceNotFoundException;
import com.tiktel.ttelgo.vendor.domain.LedgerEntry;
import com.tiktel.ttelgo.vendor.domain.Vendor;
import com.tiktel.ttelgo.vendor.infrastructure.mapper.VendorMapper;
import com.tiktel.ttelgo.vendor.infrastructure.repository.VendorJpaEntity;
import com.tiktel.ttelgo.vendor.infrastructure.repository.VendorLedgerEntryJpaEntity;
import com.tiktel.ttelgo.vendor.infrastructure.repository.VendorLedgerRepository;
import com.tiktel.ttelgo.vendor.infrastructure.repository.VendorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Vendor service with hybrid billing support (PREPAID/POSTPAID)
 */
@Slf4j
@Service
public class VendorService {
    
    private final VendorRepository vendorRepository;
    private final VendorLedgerRepository ledgerRepository;
    private final VendorMapper vendorMapper;
    
    public VendorService(VendorRepository vendorRepository,
                        VendorLedgerRepository ledgerRepository,
                        VendorMapper vendorMapper) {
        this.vendorRepository = vendorRepository;
        this.ledgerRepository = ledgerRepository;
        this.vendorMapper = vendorMapper;
    }
    
    /**
     * Get vendor by ID
     */
    public Vendor getVendorById(Long vendorId) {
        VendorJpaEntity entity = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VENDOR_NOT_FOUND, 
                        "Vendor not found with ID: " + vendorId));
        return vendorMapper.toDomain(entity);
    }
    
    /**
     * Get vendor by email
     */
    public Vendor getVendorByEmail(String email) {
        VendorJpaEntity entity = vendorRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VENDOR_NOT_FOUND, 
                        "Vendor not found with email: " + email));
        return vendorMapper.toDomain(entity);
    }
    
    /**
     * Get all vendors with pagination
     */
    public Page<Vendor> getAllVendors(Pageable pageable) {
        return vendorRepository.findAll(pageable)
                .map(vendorMapper::toDomain);
    }
    
    /**
     * Get vendors by status
     */
    public Page<Vendor> getVendorsByStatus(VendorStatus status, Pageable pageable) {
        return vendorRepository.findByStatus(status, pageable)
                .map(vendorMapper::toDomain);
    }
    
    /**
     * Search vendors with filters
     */
    public Page<Vendor> searchVendors(VendorStatus status, String search, Pageable pageable) {
        return vendorRepository.findByFilters(status, search, pageable)
                .map(vendorMapper::toDomain);
    }
    
    /**
     * Create new vendor
     */
    @Transactional
    public Vendor createVendor(Vendor vendor, Long createdBy) {
        log.info("Creating new vendor: {}", vendor.getEmail());
        
        // Check if email already exists
        if (vendorRepository.existsByEmail(vendor.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, 
                    "Vendor with email " + vendor.getEmail() + " already exists");
        }
        
        // Set defaults
        vendor.setStatus(VendorStatus.PENDING_APPROVAL);
        vendor.setWalletBalance(BigDecimal.ZERO);
        vendor.setOutstandingBalance(BigDecimal.ZERO);
        vendor.setCreditLimit(BigDecimal.ZERO);
        vendor.setRiskScore(0);
        vendor.setIsVerified(false);
        vendor.setKycCompleted(false);
        vendor.setApiEnabled(true);
        vendor.setWebhookEnabled(false);
        vendor.setCreatedBy(createdBy);
        
        VendorJpaEntity entity = vendorMapper.toEntity(vendor);
        VendorJpaEntity saved = vendorRepository.save(entity);
        
        log.info("Vendor created successfully: ID={}, email={}", saved.getId(), saved.getEmail());
        return vendorMapper.toDomain(saved);
    }
    
    /**
     * Update vendor
     */
    @Transactional
    public Vendor updateVendor(Long vendorId, Vendor updates, Long updatedBy) {
        log.info("Updating vendor: {}", vendorId);
        
        VendorJpaEntity existing = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VENDOR_NOT_FOUND, 
                        "Vendor not found with ID: " + vendorId));
        
        // Update fields (selective update)
        if (updates.getName() != null) existing.setName(updates.getName());
        if (updates.getCompanyName() != null) existing.setCompanyName(updates.getCompanyName());
        if (updates.getPhoneNumber() != null) existing.setPhoneNumber(updates.getPhoneNumber());
        if (updates.getContactPerson() != null) existing.setContactPerson(updates.getContactPerson());
        if (updates.getBillingMode() != null) existing.setBillingMode(updates.getBillingMode());
        if (updates.getCreditLimit() != null) existing.setCreditLimit(updates.getCreditLimit());
        if (updates.getDailyOrderLimit() != null) existing.setDailyOrderLimit(updates.getDailyOrderLimit());
        if (updates.getMonthlyOrderLimit() != null) existing.setMonthlyOrderLimit(updates.getMonthlyOrderLimit());
        if (updates.getDailySpendLimit() != null) existing.setDailySpendLimit(updates.getDailySpendLimit());
        if (updates.getMonthlySpendLimit() != null) existing.setMonthlySpendLimit(updates.getMonthlySpendLimit());
        if (updates.getPaymentTerms() != null) existing.setPaymentTerms(updates.getPaymentTerms());
        if (updates.getInvoiceCycleDay() != null) existing.setInvoiceCycleDay(updates.getInvoiceCycleDay());
        if (updates.getWebhookUrl() != null) existing.setWebhookUrl(updates.getWebhookUrl());
        if (updates.getWebhookSecret() != null) existing.setWebhookSecret(updates.getWebhookSecret());
        if (updates.getWebhookEnabled() != null) existing.setWebhookEnabled(updates.getWebhookEnabled());
        if (updates.getApiEnabled() != null) existing.setApiEnabled(updates.getApiEnabled());
        if (updates.getCountry() != null) existing.setCountry(updates.getCountry());
        if (updates.getTimezone() != null) existing.setTimezone(updates.getTimezone());
        if (updates.getNotes() != null) existing.setNotes(updates.getNotes());
        
        existing.setUpdatedBy(updatedBy);
        
        VendorJpaEntity saved = vendorRepository.save(existing);
        log.info("Vendor updated successfully: {}", vendorId);
        
        return vendorMapper.toDomain(saved);
    }
    
    /**
     * Approve vendor (change status to ACTIVE)
     */
    @Transactional
    public Vendor approveVendor(Long vendorId, Long approvedBy) {
        log.info("Approving vendor: {}", vendorId);
        
        VendorJpaEntity vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VENDOR_NOT_FOUND, 
                        "Vendor not found with ID: " + vendorId));
        
        if (vendor.getStatus() == VendorStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                    "Vendor is already active");
        }
        
        vendor.setStatus(VendorStatus.ACTIVE);
        vendor.setIsVerified(true);
        vendor.setUpdatedBy(approvedBy);
        
        VendorJpaEntity saved = vendorRepository.save(vendor);
        log.info("Vendor approved: {}", vendorId);
        
        return vendorMapper.toDomain(saved);
    }
    
    /**
     * Suspend vendor
     */
    @Transactional
    public Vendor suspendVendor(Long vendorId, String reason, Long suspendedBy) {
        log.warn("Suspending vendor: {} - Reason: {}", vendorId, reason);
        
        VendorJpaEntity vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VENDOR_NOT_FOUND, 
                        "Vendor not found with ID: " + vendorId));
        
        vendor.setStatus(VendorStatus.SUSPENDED);
        vendor.setNotes((vendor.getNotes() != null ? vendor.getNotes() + "\n" : "") + 
                       "Suspended: " + reason);
        vendor.setUpdatedBy(suspendedBy);
        
        VendorJpaEntity saved = vendorRepository.save(vendor);
        log.warn("Vendor suspended: {}", vendorId);
        
        return vendorMapper.toDomain(saved);
    }
    
    // ==================== BILLING OPERATIONS ====================
    
    /**
     * Top up vendor wallet (PREPAID only)
     */
    @Transactional
    public LedgerEntry topUpWallet(Long vendorId, BigDecimal amount, Long paymentId, 
                                   String description, Long createdBy) {
        log.info("Topping up wallet for vendor {}: amount={}", vendorId, amount);
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_AMOUNT, 
                    "Top-up amount must be positive");
        }
        
        VendorJpaEntity vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VENDOR_NOT_FOUND, 
                        "Vendor not found with ID: " + vendorId));
        
        if (vendor.getBillingMode() != BillingMode.PREPAID) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, 
                    "Wallet top-up is only for PREPAID vendors");
        }
        
        // Update wallet balance
        BigDecimal newBalance = vendor.getWalletBalance().add(amount);
        vendor.setWalletBalance(newBalance);
        vendorRepository.save(vendor);
        
        // Create ledger entry (CREDIT)
        LedgerEntry ledgerEntry = createLedgerEntry(
                vendorId,
                LedgerEntryType.CREDIT,
                amount,
                newBalance,
                null,
                paymentId,
                description != null ? description : "Wallet top-up",
                createdBy
        );
        
        log.info("Wallet topped up successfully: vendor={}, newBalance={}", vendorId, newBalance);
        return ledgerEntry;
    }
    
    /**
     * Debit vendor for order (PREPAID or POSTPAID)
     */
    @Transactional
    public LedgerEntry debitForOrder(Long vendorId, BigDecimal amount, Long orderId, 
                                     String description, Long createdBy) {
        log.info("Debiting vendor {} for order {}: amount={}", vendorId, orderId, amount);
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_AMOUNT, 
                    "Debit amount must be positive");
        }
        
        VendorJpaEntity vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VENDOR_NOT_FOUND, 
                        "Vendor not found with ID: " + vendorId));
        
        BigDecimal newBalance;
        
        if (vendor.getBillingMode() == BillingMode.PREPAID) {
            // Check wallet balance
            if (vendor.getWalletBalance().compareTo(amount) < 0) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_WALLET_BALANCE, 
                        String.format("Insufficient wallet balance. Available: %s, Required: %s", 
                                vendor.getWalletBalance(), amount));
            }
            
            // Deduct from wallet
            newBalance = vendor.getWalletBalance().subtract(amount);
            vendor.setWalletBalance(newBalance);
            
        } else { // POSTPAID
            // Check credit limit
            BigDecimal newOutstanding = vendor.getOutstandingBalance().add(amount);
            if (newOutstanding.compareTo(vendor.getCreditLimit()) > 0) {
                throw new BusinessException(ErrorCode.CREDIT_LIMIT_EXCEEDED, 
                        String.format("Credit limit exceeded. Limit: %s, Outstanding: %s, Order: %s", 
                                vendor.getCreditLimit(), vendor.getOutstandingBalance(), amount));
            }
            
            // Increase outstanding balance
            vendor.setOutstandingBalance(newOutstanding);
            newBalance = vendor.getCreditLimit().subtract(newOutstanding); // Available credit
        }
        
        vendorRepository.save(vendor);
        
        // Create ledger entry (DEBIT)
        LedgerEntry ledgerEntry = createLedgerEntry(
                vendorId,
                LedgerEntryType.DEBIT,
                amount,
                newBalance,
                orderId,
                null,
                description != null ? description : "Order payment",
                createdBy
        );
        
        log.info("Vendor debited successfully: vendor={}, newBalance={}", vendorId, newBalance);
        return ledgerEntry;
    }
    
    /**
     * Refund to vendor (reverse debit)
     */
    @Transactional
    public LedgerEntry refundToVendor(Long vendorId, BigDecimal amount, Long orderId, 
                                      Long originalLedgerEntryId, String description, Long createdBy) {
        log.info("Refunding vendor {} for order {}: amount={}", vendorId, orderId, amount);
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_AMOUNT, 
                    "Refund amount must be positive");
        }
        
        VendorJpaEntity vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VENDOR_NOT_FOUND, 
                        "Vendor not found with ID: " + vendorId));
        
        BigDecimal newBalance;
        
        if (vendor.getBillingMode() == BillingMode.PREPAID) {
            // Add back to wallet
            newBalance = vendor.getWalletBalance().add(amount);
            vendor.setWalletBalance(newBalance);
            
        } else { // POSTPAID
            // Reduce outstanding balance
            BigDecimal newOutstanding = vendor.getOutstandingBalance().subtract(amount);
            if (newOutstanding.compareTo(BigDecimal.ZERO) < 0) {
                newOutstanding = BigDecimal.ZERO;
            }
            vendor.setOutstandingBalance(newOutstanding);
            newBalance = vendor.getCreditLimit().subtract(newOutstanding);
        }
        
        vendorRepository.save(vendor);
        
        // Create ledger entry (REFUND)
        LedgerEntry ledgerEntry = createLedgerEntry(
                vendorId,
                LedgerEntryType.REFUND,
                amount,
                newBalance,
                orderId,
                null,
                description != null ? description : "Order refund",
                createdBy
        );
        
        // Link to original entry if provided
        if (originalLedgerEntryId != null) {
            VendorLedgerEntryJpaEntity ledgerEntity = ledgerRepository.findById(ledgerEntry.getId())
                    .orElseThrow();
            ledgerEntity.setRelatedEntryId(originalLedgerEntryId);
            ledgerRepository.save(ledgerEntity);
        }
        
        log.info("Vendor refunded successfully: vendor={}, newBalance={}", vendorId, newBalance);
        return ledgerEntry;
    }
    
    /**
     * Adjust vendor balance (admin operation)
     */
    @Transactional
    public LedgerEntry adjustBalance(Long vendorId, BigDecimal amount, String reason, Long createdBy) {
        log.info("Adjusting balance for vendor {}: amount={}, reason={}", vendorId, amount, reason);
        
        VendorJpaEntity vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VENDOR_NOT_FOUND, 
                        "Vendor not found with ID: " + vendorId));
        
        LedgerEntryType type = amount.compareTo(BigDecimal.ZERO) > 0 ? 
                LedgerEntryType.CREDIT : LedgerEntryType.DEBIT;
        BigDecimal absAmount = amount.abs();
        
        BigDecimal newBalance;
        
        if (vendor.getBillingMode() == BillingMode.PREPAID) {
            newBalance = vendor.getWalletBalance().add(amount);
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(ErrorCode.INVALID_PAYMENT_AMOUNT, 
                        "Adjustment would result in negative balance");
            }
            vendor.setWalletBalance(newBalance);
            
        } else { // POSTPAID
            BigDecimal newOutstanding = vendor.getOutstandingBalance().subtract(amount);
            if (newOutstanding.compareTo(BigDecimal.ZERO) < 0) {
                newOutstanding = BigDecimal.ZERO;
            }
            vendor.setOutstandingBalance(newOutstanding);
            newBalance = vendor.getCreditLimit().subtract(newOutstanding);
        }
        
        vendorRepository.save(vendor);
        
        // Create ledger entry (ADJUSTMENT)
        LedgerEntry ledgerEntry = createLedgerEntry(
                vendorId,
                LedgerEntryType.ADJUSTMENT,
                absAmount,
                newBalance,
                null,
                null,
                "Balance adjustment: " + reason,
                createdBy
        );
        
        log.info("Balance adjusted: vendor={}, newBalance={}", vendorId, newBalance);
        return ledgerEntry;
    }
    
    /**
     * Get vendor ledger entries
     */
    public Page<LedgerEntry> getVendorLedger(Long vendorId, Pageable pageable) {
        return ledgerRepository.findByVendorIdOrderByCreatedAtDesc(vendorId, pageable)
                .map(vendorMapper::toLedgerDomain);
    }
    
    /**
     * Get vendor ledger with filters
     */
    public Page<LedgerEntry> getVendorLedgerFiltered(Long vendorId, LedgerEntryType type,
                                                      LocalDateTime startDate, LocalDateTime endDate,
                                                      Pageable pageable) {
        return ledgerRepository.findByFilters(vendorId, type, startDate, endDate, pageable)
                .map(vendorMapper::toLedgerDomain);
    }
    
    /**
     * Calculate vendor balance from ledger (reconciliation)
     */
    public BigDecimal calculateBalanceFromLedger(Long vendorId) {
        BigDecimal calculated = ledgerRepository.calculateBalance(vendorId);
        return calculated != null ? calculated : BigDecimal.ZERO;
    }
    
    /**
     * Check if vendor can place order
     */
    public void validateVendorCanPlaceOrder(Long vendorId, BigDecimal orderAmount) {
        Vendor vendor = getVendorById(vendorId);
        
        // Check vendor status
        if (!vendor.canPlaceOrders()) {
            if (vendor.getStatus() == VendorStatus.SUSPENDED) {
                throw new BusinessException(ErrorCode.VENDOR_SUSPENDED, 
                        "Vendor account is suspended");
            } else if (vendor.getStatus() == VendorStatus.PENDING_APPROVAL) {
                throw new BusinessException(ErrorCode.VENDOR_PENDING_APPROVAL, 
                        "Vendor account is pending approval");
            } else {
                throw new BusinessException(ErrorCode.VENDOR_NOT_ACTIVE, 
                        "Vendor account is not active");
            }
        }
        
        // Check balance
        if (!vendor.hasSufficientBalance(orderAmount)) {
            if (vendor.getBillingMode() == BillingMode.PREPAID) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_WALLET_BALANCE, 
                        String.format("Insufficient wallet balance. Available: %s, Required: %s", 
                                vendor.getWalletBalance(), orderAmount));
            } else {
                throw new BusinessException(ErrorCode.CREDIT_LIMIT_EXCEEDED, 
                        String.format("Credit limit exceeded. Available: %s, Required: %s", 
                                vendor.getAvailableBalance(), orderAmount));
            }
        }
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    private LedgerEntry createLedgerEntry(Long vendorId, LedgerEntryType type, BigDecimal amount,
                                          BigDecimal balanceAfter, Long orderId, Long paymentId,
                                          String description, Long createdBy) {
        String referenceNumber = generateReferenceNumber(type);
        
        VendorLedgerEntryJpaEntity entity = VendorLedgerEntryJpaEntity.builder()
                .vendorId(vendorId)
                .type(type)
                .amount(amount)
                .currency("USD")
                .balanceAfter(balanceAfter)
                .status(LedgerEntryStatus.COMPLETED)
                .orderId(orderId)
                .paymentId(paymentId)
                .description(description)
                .referenceNumber(referenceNumber)
                .createdBy(createdBy)
                .build();
        
        VendorLedgerEntryJpaEntity saved = ledgerRepository.save(entity);
        return vendorMapper.toLedgerDomain(saved);
    }
    
    private String generateReferenceNumber(LedgerEntryType type) {
        String prefix = switch (type) {
            case CREDIT -> "CR";
            case DEBIT -> "DB";
            case REFUND -> "RF";
            case ADJUSTMENT -> "ADJ";
            case REVERSAL -> "REV";
        };
        return prefix + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}

