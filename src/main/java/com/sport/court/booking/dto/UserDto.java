package com.sport.court.booking.dto;

import com.sport.court.booking.domain.Role;
import com.sport.court.booking.domain.User;

public record UserDto(Long id, String firstName, String lastName, String email, Role role) {
    public static UserDto from(User u) {
        return new UserDto(u.getId(), u.getFirstName(), u.getLastName(), u.getEmail(), u.getRole());
    }
}
