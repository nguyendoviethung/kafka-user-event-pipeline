package com.example.servera.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DepartmentRequest {
    @NotBlank(message = "Tên phòng ban không được để trống")
    private String name;
    private String description;

    @NotNull(message = "organizationId không được để trống")
    private Long organizationId;
}
