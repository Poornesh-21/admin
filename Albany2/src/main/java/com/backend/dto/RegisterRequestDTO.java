package com.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//Register Request DTO
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDTO {
 private String email;
 private String firstName;
 private String lastName;
 private String phoneNumber;
}
