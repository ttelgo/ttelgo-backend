package com.tiktel.ttelgo.esim.api.dto;

import lombok.Data;

@Data
public class EsimQrResponse {
    private String qrCode;
    private String matchingId;
    private String iccid;
}

