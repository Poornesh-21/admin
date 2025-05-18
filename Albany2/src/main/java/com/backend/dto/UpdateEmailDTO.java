package com.backend.dto;

import lombok.Data;

@Data
public class UpdateEmailDTO {
    private String newEmail;
    private String otp;
}