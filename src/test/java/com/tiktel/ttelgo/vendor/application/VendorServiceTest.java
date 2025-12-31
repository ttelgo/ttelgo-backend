package com.tiktel.ttelgo.vendor.application;

import com.tiktel.ttelgo.common.domain.enums.BillingMode;
import com.tiktel.ttelgo.common.domain.enums.VendorStatus;
import com.tiktel.ttelgo.common.exception.BusinessException;
import com.tiktel.ttelgo.vendor.domain.Vendor;
import com.tiktel.ttelgo.vendor.infrastructure.mapper.VendorMapper;
import com.tiktel.ttelgo.vendor.infrastructure.repository.VendorJpaEntity;
import com.tiktel.ttelgo.vendor.infrastructure.repository.VendorLedgerRepository;
import com.tiktel.ttelgo.vendor.infrastructure.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VendorService
 */
@ExtendWith(MockitoExtension.class)
class VendorServiceTest {
    
    @Mock
    private VendorRepository vendorRepository;
    
    @Mock
    private VendorLedgerRepository ledgerRepository;
    
    @Mock
    private VendorMapper vendorMapper;
    
    @InjectMocks
    private VendorService vendorService;
    
    private VendorJpaEntity testVendorEntity;
    private Vendor testVendor;
    
    @BeforeEach
    void setUp() {
        testVendorEntity = VendorJpaEntity.builder()
                .id(1L)
                .email("test@vendor.com")
                .name("Test Vendor")
                .companyName("Test Company")
                .billingMode(BillingMode.PREPAID)
                .status(VendorStatus.ACTIVE)
                .walletBalance(BigDecimal.valueOf(1000))
                .creditLimit(BigDecimal.ZERO)
                .outstandingBalance(BigDecimal.ZERO)
                .isVerified(true)
                .apiEnabled(true)
                .build();
        
        testVendor = Vendor.builder()
                .id(1L)
                .email("test@vendor.com")
                .name("Test Vendor")
                .companyName("Test Company")
                .billingMode(BillingMode.PREPAID)
                .status(VendorStatus.ACTIVE)
                .walletBalance(BigDecimal.valueOf(1000))
                .creditLimit(BigDecimal.ZERO)
                .outstandingBalance(BigDecimal.ZERO)
                .isVerified(true)
                .apiEnabled(true)
                .build();
    }
    
    @Test
    void testGetVendorById_Success() {
        // Arrange
        when(vendorRepository.findById(1L)).thenReturn(Optional.of(testVendorEntity));
        when(vendorMapper.toDomain(testVendorEntity)).thenReturn(testVendor);
        
        // Act
        Vendor result = vendorService.getVendorById(1L);
        
        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("test@vendor.com", result.getEmail());
        verify(vendorRepository).findById(1L);
    }
    
    @Test
    void testCreateVendor_Success() {
        // Arrange
        when(vendorRepository.existsByEmail("new@vendor.com")).thenReturn(false);
        when(vendorMapper.toEntity(any(Vendor.class))).thenReturn(testVendorEntity);
        when(vendorRepository.save(any(VendorJpaEntity.class))).thenReturn(testVendorEntity);
        when(vendorMapper.toDomain(testVendorEntity)).thenReturn(testVendor);
        
        // Act
        Vendor result = vendorService.createVendor(testVendor, 1L);
        
        // Assert
        assertNotNull(result);
        assertEquals(VendorStatus.PENDING_APPROVAL, testVendor.getStatus());
        verify(vendorRepository).save(any(VendorJpaEntity.class));
    }
    
    @Test
    void testCreateVendor_DuplicateEmail_ThrowsException() {
        // Arrange
        when(vendorRepository.existsByEmail("test@vendor.com")).thenReturn(true);
        
        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            vendorService.createVendor(testVendor, 1L);
        });
        
        verify(vendorRepository, never()).save(any());
    }
    
    @Test
    void testTopUpWallet_Success() {
        // Arrange
        BigDecimal topUpAmount = BigDecimal.valueOf(500);
        when(vendorRepository.findById(1L)).thenReturn(Optional.of(testVendorEntity));
        when(vendorRepository.save(any(VendorJpaEntity.class))).thenReturn(testVendorEntity);
        when(ledgerRepository.save(any())).thenReturn(null);
        when(vendorMapper.toLedgerDomain(any())).thenReturn(null);
        
        // Act
        vendorService.topUpWallet(1L, topUpAmount, 1L, "Test top-up", 1L);
        
        // Assert
        verify(vendorRepository).save(argThat(vendor ->
                vendor.getWalletBalance().equals(BigDecimal.valueOf(1500))
        ));
    }
    
    @Test
    void testValidateVendorCanPlaceOrder_InsufficientBalance_ThrowsException() {
        // Arrange
        when(vendorRepository.findById(1L)).thenReturn(Optional.of(testVendorEntity));
        when(vendorMapper.toDomain(testVendorEntity)).thenReturn(testVendor);
        
        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            vendorService.validateVendorCanPlaceOrder(1L, BigDecimal.valueOf(2000));
        });
    }
    
    @Test
    void testValidateVendorCanPlaceOrder_Success() {
        // Arrange
        when(vendorRepository.findById(1L)).thenReturn(Optional.of(testVendorEntity));
        when(vendorMapper.toDomain(testVendorEntity)).thenReturn(testVendor);
        
        // Act & Assert - should not throw
        assertDoesNotThrow(() -> {
            vendorService.validateVendorCanPlaceOrder(1L, BigDecimal.valueOf(500));
        });
    }
}

