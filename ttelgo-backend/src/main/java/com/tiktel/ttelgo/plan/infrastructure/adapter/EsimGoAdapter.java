package com.tiktel.ttelgo.plan.infrastructure.adapter;

import com.tiktel.ttelgo.integration.esimgo.EsimGoClient;
import com.tiktel.ttelgo.integration.esimgo.dto.BundleResponse;
import com.tiktel.ttelgo.plan.application.port.EsimGoPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EsimGoAdapter implements EsimGoPort {

    private final EsimGoClient esimGoClient;

    @Override
    public BundleResponse listBundles() {
        return esimGoClient.listBundles();
    }

    @Override
    public BundleResponse listBundles(Integer page, Integer perPage, String direction, String orderBy, String description) {
        return esimGoClient.listBundles(page, perPage, direction, orderBy, description);
    }

    @Override
    public BundleResponse listBundlesByCountry(String countryIso) {
        return esimGoClient.listBundlesByCountry(countryIso);
    }

    @Override
    public BundleResponse.Bundle getBundleDetails(String bundleName) {
        return esimGoClient.getBundleDetails(bundleName);
    }
}

