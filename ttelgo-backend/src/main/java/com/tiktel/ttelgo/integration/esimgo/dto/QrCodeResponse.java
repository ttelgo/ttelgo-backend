package com.tiktel.ttelgo.integration.esimgo.dto;

import lombok.Data;

@Data
public class QrCodeResponse {
    private String qrCode;
    private String matchingId;
    private String iccid;
}

