package com.lifeos.travel.dto;

import com.lifeos.travel.entity.BookingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBookingStatusRequest {

    @NotNull(message = "Status is required")
    private BookingStatus status;

    private String notes;
}
