package com.frontend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//User Response DTO
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {
 private Long id;
 private String email;
 private String firstName;
 private String lastName;
 private String phoneNumber;
 private String street;
 private String city;
 private String state;
 private String postalCode;
 private String membershipType;
 private String role;
}