package com.bankops.modules.user;

import com.bankops.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "User Administration", description = "Endpoints for managing system staff, analysts, and access control roles")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "List all internal staff users")
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success(userService.getAllUsers(), "Users retrieved successfully"));
    }

    @PostMapping
    @Operation(summary = "Create a new staff member account")
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserDto created = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "User account created successfully"));
    }

    @PatchMapping("/{id}/role")
    @Operation(summary = "Assign or update staff member roles")
    public ResponseEntity<ApiResponse<UserDto>> updateUserRoles(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleRequest request) {
        UserDto updated = userService.updateUserRoles(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "User roles updated successfully"));
    }
}
