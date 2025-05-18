package com.albany.mvc.dto.customer;

import lombok.Data;

@Data
public class UpdatePhoneDTO {
    private String newPhone;
    private String otp;
}