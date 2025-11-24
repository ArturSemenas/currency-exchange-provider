package com.currencyexchange.provider.service;

import com.currencyexchange.provider.model.Role;
import com.currencyexchange.provider.model.User;
import com.currencyexchange.provider.repository.RoleRepository;
import com.currencyexchange.provider.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Service for user management operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Find user by username
     *
     * @param username the username to search for
     * @return Optional containing the user if found
     */
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        log.debug("Finding user by username: {}", username);
        return userRepository.findByUsernameWithRoles(username);
    }

    /**
     * Check if username exists
     *
     * @param username the username to check
     * @return true if username exists
     */
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Create a new user with specified roles
     *
     * @param username the username
     * @param password the plain text password (will be encoded)
     * @param roleNames the role names to assign
     * @return the created user
     * @throws IllegalArgumentException if username already exists or roles not found
     */
    @Transactional
    public User createUser(String username, String password, Set<String> roleNames) {
        log.info("Creating new user: {}", username);

        if (existsByUsername(username)) {
            log.warn("Username already exists: {}", username);
            throw new IllegalArgumentException("Username already exists: " + username);
        }

        // Find roles
        Set<Role> roles = new HashSet<>();
        for (String roleName : roleNames) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
            roles.add(role);
        }

        // Create user with encoded password
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .enabled(true)
                .roles(roles)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User created successfully: {} with {} roles", username, roles.size());

        return savedUser;
    }

    /**
     * Enable or disable a user
     *
     * @param username the username
     * @param enabled true to enable, false to disable
     * @throws IllegalArgumentException if user not found
     */
    @Transactional
    public void setUserEnabled(String username, boolean enabled) {
        log.info("Setting user {} enabled status to: {}", username, enabled);

        User user = userRepository.findByUsernameWithRoles(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        user.setEnabled(enabled);
        userRepository.save(user);

        log.info("User {} enabled status updated to: {}", username, enabled);
    }

    /**
     * Add role to user
     *
     * @param username the username
     * @param roleName the role name to add
     * @throws IllegalArgumentException if user or role not found
     */
    @Transactional
    public void addRoleToUser(String username, String roleName) {
        log.info("Adding role {} to user {}", roleName, username);

        User user = userRepository.findByUsernameWithRoles(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));

        user.getRoles().add(role);
        userRepository.save(user);

        log.info("Role {} added to user {}", roleName, username);
    }

    /**
     * Remove role from user
     *
     * @param username the username
     * @param roleName the role name to remove
     * @throws IllegalArgumentException if user or role not found
     */
    @Transactional
    public void removeRoleFromUser(String username, String roleName) {
        log.info("Removing role {} from user {}", roleName, username);

        User user = userRepository.findByUsernameWithRoles(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));

        user.getRoles().remove(role);
        userRepository.save(user);

        log.info("Role {} removed from user {}", roleName, username);
    }

    /**
     * Get all users
     *
     * @return list of all users
     */
    @Transactional(readOnly = true)
    public Iterable<User> getAllUsers() {
        log.debug("Retrieving all users");
        return userRepository.findAll();
    }
}
