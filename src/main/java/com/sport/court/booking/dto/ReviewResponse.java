package com.sport.court.booking.dto;

import com.sport.court.booking.domain.Review;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long courtId,
        String userName,
        String userInitials,
        Integer score,
        String comment,
        LocalDateTime createdAt
) {
    public static ReviewResponse from(Review r) {
        String fn = r.getUser().getFirstName() != null ? r.getUser().getFirstName() : "";
        String ln = r.getUser().getLastName() != null ? r.getUser().getLastName() : "";
        String name = (fn + " " + ln).trim();
        String i1 = !fn.isEmpty() ? fn.substring(0, 1) : "";
        String i2 = !ln.isEmpty() ? ln.substring(0, 1) : "";
        String initials = (i1 + i2).toUpperCase();
        if (initials.isEmpty()) initials = "U";
        return new ReviewResponse(
                r.getId(),
                r.getCourt().getId(),
                name,
                initials,
                r.getScore(),
                r.getComment(),
                r.getCreatedAt()
        );
    }
}
