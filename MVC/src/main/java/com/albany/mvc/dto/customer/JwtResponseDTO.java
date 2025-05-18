package com.albany.mvc.dto.customer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//JWT Response DTO
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponseDTO {
 private String token;
 private UserResponseDTO user;
}
