package com.example.servera.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoleResponse {
    private Long id;
    private String name;
    private String description;
}
