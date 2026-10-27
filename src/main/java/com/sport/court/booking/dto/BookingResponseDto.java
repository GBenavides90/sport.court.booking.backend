package com.sport.court.booking.dto;

import com.sport.court.booking.domain.Booking;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponseDto {

    private Long id;
    private String bookingCode;

    private Long courtId;
    private String courtName;
    private String courtImageUrl;
    private String courtCategoryName;
    private String courtDescription;

    private Long userId;
    private String userFirstName;
    private String userLastName;
    private String userEmail;

    private LocalDate startDate;
    private LocalDate endDate;
    private String startTime;
    private String endTime;

    private String notes;
    private String status;
    private LocalDateTime createdAt;

    public static BookingResponseDto fromEntity(Booking b) {
        if (b == null) return null;
        return BookingResponseDto.builder()
                .id(b.getId())
                .bookingCode(b.getBookingCode())
                .courtId(b.getCourt() != null ? b.getCourt().getId() : null)
                .courtName(b.getCourt() != null ? b.getCourt().getName() : null)
                .courtImageUrl(b.getCourt() != null ? b.getCourt().getImageUrl() : null)
                .courtCategoryName(b.getCourt() != null && b.getCourt().getCategory() != null ? b.getCourt().getCategory().getTitle() : null)
                .courtDescription(b.getCourt() != null ? b.getCourt().getDescription() : null)
                .userId(b.getUser() != null ? b.getUser().getId() : null)
                .userFirstName(b.getUser() != null ? b.getUser().getFirstName() : null)
                .userLastName(b.getUser() != null ? b.getUser().getLastName() : null)
                .userEmail(b.getUser() != null ? b.getUser().getEmail() : null)
                .startDate(b.getStartDate())
                .endDate(b.getEndDate())
                .startTime(b.getStartTime())
                .endTime(b.getEndTime())
                .notes(b.getNotes())
                .status(b.getStatus())
                .createdAt(b.getCreatedAt())
                .build();
    }
}
