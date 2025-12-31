package com.tiktel.ttelgo.esim.infrastructure.adapter;

import com.tiktel.ttelgo.common.domain.enums.EsimStatus;
import com.tiktel.ttelgo.esim.application.port.EsimRepositoryPort;
import com.tiktel.ttelgo.esim.domain.Esim;
import com.tiktel.ttelgo.esim.infrastructure.mapper.EsimMapper;
import com.tiktel.ttelgo.esim.infrastructure.repository.EsimJpaEntity;
import com.tiktel.ttelgo.esim.infrastructure.repository.EsimRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class EsimRepositoryAdapter implements EsimRepositoryPort {
    
    private final EsimRepository esimRepository;
    private final EsimMapper esimMapper;
    
    @Autowired
    public EsimRepositoryAdapter(EsimRepository esimRepository, EsimMapper esimMapper) {
        this.esimRepository = esimRepository;
        this.esimMapper = esimMapper;
    }
    
    @Override
    public Esim save(Esim esim) {
        var entity = esimMapper.toEntity(esim);
        var savedEntity = esimRepository.save(entity);
        return esimMapper.toDomain(savedEntity);
    }
    
    @Override
    public Optional<Esim> findById(Long id) {
        return esimRepository.findById(id)
                .map(esimMapper::toDomain);
    }
    
    @Override
    public Optional<Esim> findByEsimUuid(String esimUuid) {
        // TODO: Implement if esimUuid field exists, otherwise return empty
        // For now, returning empty as esimUuid is not in the current schema
        return Optional.empty();
    }
    
    @Override
    public Optional<Esim> findByMatchingId(String matchingId) {
        return esimRepository.findByMatchingId(matchingId)
                .map(esimMapper::toDomain);
    }
    
    @Override
    public Optional<Esim> findByIccid(String iccid) {
        return esimRepository.findByIccid(iccid)
                .map(esimMapper::toDomain);
    }
    
    @Override
    public Optional<Esim> findByOrderId(Long orderId) {
        List<EsimJpaEntity> entities = esimRepository.findByOrderId(orderId);
        // Return first eSIM if exists (or could return all, but interface says Optional)
        return entities.isEmpty() 
                ? Optional.empty() 
                : Optional.of(esimMapper.toDomain(entities.get(0)));
    }
    
    @Override
    public List<Esim> findByUserId(Long userId) {
        return esimRepository.findByUserId(userId, org.springframework.data.domain.Pageable.unpaged())
                .getContent()
                .stream()
                .map(esimMapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Esim> findByStatus(EsimStatus status) {
        return esimRepository.findByStatus(status, org.springframework.data.domain.Pageable.unpaged())
                .getContent()
                .stream()
                .map(esimMapper::toDomain)
                .collect(Collectors.toList());
    }
}

