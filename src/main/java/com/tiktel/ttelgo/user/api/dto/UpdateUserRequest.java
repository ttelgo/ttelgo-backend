package com.tiktel.ttelgo.user.api.dto;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String firstName;
    private String lastName;
    private String country;
    private String city;
    private String address;
    private String postalCode;
    private String phone;
}

