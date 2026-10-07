package com.sport.court.booking.service;

import com.sport.court.booking.domain.Role;
import com.sport.court.booking.domain.User;
import com.sport.court.booking.dto.UserDto;
import com.sport.court.booking.exception.ConflictException;
import com.sport.court.booking.exception.NotFoundException;
import com.sport.court.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** HU16 — gestión de usuarios y permisos de administrador. */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<UserDto> listUsers() {
        return userRepository.findAll().stream().map(UserDto::from).toList();
    }

    /** Asigna o retira el permiso de administrador. Un administrador no puede quitarse el permiso a sí mismo. */
    public UserDto changeRole(Long targetId, Role newRole, User actor) {
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new NotFoundException("El usuario no existe"));
        if (target.getId().equals(actor.getId()) && newRole != Role.ADMIN) {
            throw new ConflictException("No puedes retirarte el permiso de administrador a ti mismo");
        }
        target.setRole(newRole);
        return UserDto.from(userRepository.save(target));
    }
}
