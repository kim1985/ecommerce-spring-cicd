package com.myecom.dto.product;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO per la creazione/aggiornamento categoria
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {

    @NotBlank(message = "Nome categoria è obbligatorio")
    private String name;

    private String description;
    private Boolean active = true;
}