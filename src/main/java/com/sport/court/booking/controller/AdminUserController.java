package com.sport.court.booking.controller;

import com.sport.court.booking.domain.User;
import com.sport.court.booking.dto.RoleRequest;
import com.sport.court.booking.dto.UserDto;
import com.sport.court.booking.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin Users", description = "Gestión de usuarios y permisos de administrador (HU16) — solo ADMIN")
public class AdminUserController {

    private final UserService userService;

    @Operation(summary = "Listar usuarios registrados")
    @GetMapping
    public ResponseEntity<List<UserDto>> list() {
        return ResponseEntity.ok(userService.listUsers());
    }

    @Operation(summary = "Asignar o retirar el permiso de administrador")
    @PutMapping("/{id}/role")
    public ResponseEntity<UserDto> changeRole(@PathVariable Long id,
                                              @Valid @RequestBody RoleRequest request,
                                              @AuthenticationPrincipal User actor) {
        return ResponseEntity.ok(userService.changeRole(id, request.role(), actor));
    }
}
