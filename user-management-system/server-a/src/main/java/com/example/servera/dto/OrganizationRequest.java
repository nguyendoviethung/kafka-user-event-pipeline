package com.example.servera.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrganizationRequest {
    @NotBlank(message = "Tên tổ chức không được để trống")
    private String name;
    private String description;
}
