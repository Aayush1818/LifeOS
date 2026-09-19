package com.lifeos.travel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddTravelerRequest {

    @NotBlank(message = "Traveler name is required")
    @Size(max = 150, message = "Traveler name cannot exceed 150 characters")
    private String travelerName;

    private UUID dependentId;

    @Builder.Default
    private boolean isPrimaryUser = false;

    @Size(max = 255, message = "Notes cannot exceed 255 characters")
    private String notes;
}
