package com.tiktel.ttelgo.kyc.infrastructure.adapter;

import com.tiktel.ttelgo.kyc.application.port.KycRepositoryPort;
import com.tiktel.ttelgo.kyc.domain.Kyc;
import com.tiktel.ttelgo.kyc.infrastructure.repository.KycRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class KycRepositoryAdapter implements KycRepositoryPort {
    
    private final KycRepository kycRepository;
    
    @Autowired
    public KycRepositoryAdapter(KycRepository kycRepository) {
        this.kycRepository = kycRepository;
    }
    
    @Override
    public Kyc save(Kyc kyc) {
        return kycRepository.save(kyc);
    }
    
    @Override
    public Optional<Kyc> findById(Long id) {
        return kycRepository.findById(id);
    }
    
    @Override
    public Optional<Kyc> findByUserId(Long userId) {
        return kycRepository.findByUserId(userId);
    }
    
    @Override
    public List<Kyc> findByStatus(com.tiktel.ttelgo.kyc.domain.KycStatus status) {
        return kycRepository.findByStatus(status);
    }
    
    @Override
    public boolean existsByUserId(Long userId) {
        return kycRepository.existsByUserId(userId);
    }
}

