package com.albany.mvc.dto.customer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//OTP Verification DTO
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerificationDTO {
 private String email;
 private String otp;
}