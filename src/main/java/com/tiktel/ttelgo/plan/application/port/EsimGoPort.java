package com.tiktel.ttelgo.plan.application.port;

import com.tiktel.ttelgo.integration.esimgo.dto.BundleResponse;

public interface EsimGoPort {

    BundleResponse listBundles();

    BundleResponse listBundles(Integer page, Integer perPage, String direction, String orderBy, String description);

    BundleResponse listBundlesByCountry(String countryIso);

    BundleResponse.Bundle getBundleDetails(String bundleName);
}

