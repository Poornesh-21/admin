package com.frontend.dto;

import lombok.Data;

@Data
public class UpdateEmailDTO {
    private String newEmail;
    private String otp;
}