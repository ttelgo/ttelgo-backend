package com.tiktel.ttelgo.kyc.api.dto;

import lombok.Data;

@Data
public class KycUploadRequest {
    private String documentType; // passport, id_card, driver_license
    private String documentNumber;
    private String documentFrontUrl;
    private String documentBackUrl;
    private String selfieUrl;
    private String firstName;
    private String lastName;
    private String dateOfBirth; // ISO date string
    private String nationality;
    private String address;
}

