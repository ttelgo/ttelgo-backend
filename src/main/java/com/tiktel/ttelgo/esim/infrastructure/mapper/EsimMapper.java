package com.tiktel.ttelgo.esim.infrastructure.mapper;

import com.tiktel.ttelgo.esim.domain.Esim;
import com.tiktel.ttelgo.esim.infrastructure.repository.EsimJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class EsimMapper {
    
    public Esim toDomain(EsimJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return Esim.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .userId(entity.getUserId())
                .vendorId(entity.getVendorId())
                .iccid(entity.getIccid())
                .matchingId(entity.getMatchingId())
                .smdpAddress(entity.getSmdpAddress())
                .activationCode(entity.getActivationCode())
                .qrCode(entity.getQrCode())
                .qrCodeUrl(entity.getQrCodeUrl())
                .bundleCode(entity.getBundleCode())
                .bundleName(entity.getBundleName())
                .status(entity.getStatus())
                .dataLimitBytes(entity.getDataLimitBytes())
                .dataUsedBytes(entity.getDataUsedBytes())
                .dataRemainingBytes(entity.getDataRemainingBytes())
                .validFrom(entity.getValidFrom())
                .validUntil(entity.getValidUntil())
                .validityDays(entity.getValidityDays())
                .activatedAt(entity.getActivatedAt())
                .activationCodeIos(entity.getActivationCodeIos())
                .activationCodeAndroid(entity.getActivationCodeAndroid())
                .countryIso(entity.getCountryIso())
                .networkName(entity.getNetworkName())
                .lastSyncedAt(entity.getLastSyncedAt())
                .syncStatus(entity.getSyncStatus())
                .syncError(entity.getSyncError())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .expiredAt(entity.getExpiredAt())
                .build();
    }
    
    public EsimJpaEntity toEntity(Esim domain) {
        if (domain == null) {
            return null;
        }
        
        return EsimJpaEntity.builder()
                .id(domain.getId())
                .orderId(domain.getOrderId())
                .userId(domain.getUserId())
                .vendorId(domain.getVendorId())
                .iccid(domain.getIccid())
                .matchingId(domain.getMatchingId())
                .smdpAddress(domain.getSmdpAddress())
                .activationCode(domain.getActivationCode())
                .qrCode(domain.getQrCode())
                .qrCodeUrl(domain.getQrCodeUrl())
                .bundleCode(domain.getBundleCode())
                .bundleName(domain.getBundleName())
                .status(domain.getStatus())
                .dataLimitBytes(domain.getDataLimitBytes())
                .dataUsedBytes(domain.getDataUsedBytes())
                .dataRemainingBytes(domain.getDataRemainingBytes())
                .validFrom(domain.getValidFrom())
                .validUntil(domain.getValidUntil())
                .validityDays(domain.getValidityDays())
                .activatedAt(domain.getActivatedAt())
                .activationCodeIos(domain.getActivationCodeIos())
                .activationCodeAndroid(domain.getActivationCodeAndroid())
                .countryIso(domain.getCountryIso())
                .networkName(domain.getNetworkName())
                .lastSyncedAt(domain.getLastSyncedAt())
                .syncStatus(domain.getSyncStatus())
                .syncError(domain.getSyncError())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .expiredAt(domain.getExpiredAt())
                .build();
    }
}

