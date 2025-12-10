package com.tiktel.ttelgo.esim.infrastructure.adapter;

import com.tiktel.ttelgo.esim.application.port.EsimRepositoryPort;
import com.tiktel.ttelgo.esim.domain.Esim;
import com.tiktel.ttelgo.esim.infrastructure.repository.EsimRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class EsimRepositoryAdapter implements EsimRepositoryPort {
    
    private final EsimRepository esimRepository;
    
    @Autowired
    public EsimRepositoryAdapter(EsimRepository esimRepository) {
        this.esimRepository = esimRepository;
    }
    
    @Override
    public Esim save(Esim esim) {
        return esimRepository.save(esim);
    }
    
    @Override
    public Optional<Esim> findById(Long id) {
        return esimRepository.findById(id);
    }
    
    @Override
    public Optional<Esim> findByEsimUuid(String esimUuid) {
        return esimRepository.findByEsimUuid(esimUuid);
    }
    
    @Override
    public Optional<Esim> findByMatchingId(String matchingId) {
        return esimRepository.findByMatchingId(matchingId);
    }
    
    @Override
    public Optional<Esim> findByIccid(String iccid) {
        return esimRepository.findByIccid(iccid);
    }
    
    @Override
    public Optional<Esim> findByOrderId(Long orderId) {
        return esimRepository.findByOrderId(orderId);
    }
    
    @Override
    public List<Esim> findByUserId(Long userId) {
        return esimRepository.findByUserId(userId);
    }
    
    @Override
    public List<Esim> findByStatus(com.tiktel.ttelgo.esim.domain.EsimStatus status) {
        return esimRepository.findByStatus(status);
    }
}

