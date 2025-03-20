package com.aptio.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerNoteDTO {
    private String id;

    @NotBlank(message = "Content is required")
    private String content;

    @NotBlank(message = "Created by is required")
    private String createdBy;
}