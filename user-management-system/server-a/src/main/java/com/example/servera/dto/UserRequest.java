package com.example.servera.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserRequest {

    @NotBlank(message = "username không được để trống")
    private String username;

    @NotBlank(message = "email không được để trống")
    @Email(message = "email không đúng định dạng")
    private String email;

    private String fullName;

    @NotNull(message = "roleId không được để trống")
    private Long roleId;

    @NotNull(message = "departmentId không được để trống")
    private Long departmentId;
}
