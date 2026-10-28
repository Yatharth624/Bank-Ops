package com.bankops.modules.user;

import com.bankops.common.DuplicateResourceException;
import com.bankops.common.ResourceNotFoundException;
import com.bankops.modules.audit.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(UUID id) {
        return userRepository.findById(id)
                .map(UserDto::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already registered");
        }

        Set<Role> roles = new HashSet<>();
        for (String roleName : request.getRoles()) {
            Role role = roleRepository.findByName(roleName.toUpperCase())
                    .orElseGet(() -> roleRepository.save(Role.builder().name(roleName.toUpperCase()).build()));
            roles.add(role);
        }

        User user = User.builder()
                .username(request.getUsername().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .active(true)
                .roles(roles)
                .build();

        User saved = userRepository.save(user);

        auditService.logEvent("CREATE_USER", "USER", saved.getId().toString(),
                String.format("Created user '%s' with roles: %s", saved.getUsername(), request.getRoles()), "SUCCESS");

        return UserDto.fromEntity(saved);
    }

    @Transactional
    public UserDto updateUserRoles(UUID id, UpdateRoleRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        Set<Role> roles = new HashSet<>();
        for (String roleName : request.getRoles()) {
            Role role = roleRepository.findByName(roleName.toUpperCase())
                    .orElseGet(() -> roleRepository.save(Role.builder().name(roleName.toUpperCase()).build()));
            roles.add(role);
        }

        user.setRoles(roles);
        User updated = userRepository.save(user);

        auditService.logEvent("UPDATE_USER_ROLES", "USER", updated.getId().toString(),
                String.format("Updated user '%s' roles to: %s", updated.getUsername(), request.getRoles()), "SUCCESS");

        return UserDto.fromEntity(updated);
    }
}
