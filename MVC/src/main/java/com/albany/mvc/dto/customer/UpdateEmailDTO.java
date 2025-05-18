package com.albany.mvc.dto.customer;

import lombok.Data;

@Data
public class UpdateEmailDTO {
    private String newEmail;
    private String otp;
}