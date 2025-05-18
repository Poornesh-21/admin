package com.backend.dto;

import lombok.Data;

@Data
public class UpdatePhoneDTO {
    private String newPhone;
    private String otp;
}