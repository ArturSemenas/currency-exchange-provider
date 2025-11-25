package com.currencyexchange.provider.service;

import com.currencyexchange.provider.model.Role;
import com.currencyexchange.provider.model.User;
import com.currencyexchange.provider.repository.RoleRepository;
import com.currencyexchange.provider.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService.
 * Tests all business logic with mocked dependencies
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Role userRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        userRole = Role.builder()
                .id(1L)
                .name("ROLE_USER")
                .build();

        adminRole = Role.builder()
                .id(2L)
                .name("ROLE_ADMIN")
                .build();

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .password("encodedPassword123")
                .enabled(true)
                .roles(roles)
                .build();
    }

    @Test
    @DisplayName("Should find user by username successfully")
    void findByUsername_ShouldReturnUser_WhenUserExists() {
        // Arrange
        when(userRepository.findByUsernameWithRoles("testuser"))
                .thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.findByUsername("testuser");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser");
        assertThat(result.get().getRoles()).hasSize(1);
        verify(userRepository).findByUsernameWithRoles("testuser");
    }

    @Test
    @DisplayName("Should return empty optional when user not found")
    void findByUsername_ShouldReturnEmpty_WhenUserNotFound() {
        // Arrange
        when(userRepository.findByUsernameWithRoles("nonexistent"))
                .thenReturn(Optional.empty());

        // Act
        Optional<User> result = userService.findByUsername("nonexistent");

        // Assert
        assertThat(result).isEmpty();
        verify(userRepository).findByUsernameWithRoles("nonexistent");
    }

    @Test
    @DisplayName("Should return true when username exists")
    void existsByUsername_ShouldReturnTrue_WhenUsernameExists() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // Act
        boolean result = userService.existsByUsername("testuser");

        // Assert
        assertThat(result).isTrue();
        verify(userRepository).existsByUsername("testuser");
    }

    @Test
    @DisplayName("Should return false when username does not exist")
    void existsByUsername_ShouldReturnFalse_WhenUsernameNotExists() {
        // Arrange
        when(userRepository.existsByUsername("newuser")).thenReturn(false);

        // Act
        boolean result = userService.existsByUsername("newuser");

        // Assert
        assertThat(result).isFalse();
        verify(userRepository).existsByUsername("newuser");
    }

    @Test
    @DisplayName("Should create user successfully with roles")
    void createUser_ShouldCreateUser_WhenValidData() {
        // Arrange
        String username = "newuser";
        String password = "password123";
        Set<String> roleNames = Set.of("ROLE_USER");

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(password)).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return user;
        });

        // Act
        User result = userService.createUser(username, password, roleNames);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(username);
        assertThat(result.getPassword()).isEqualTo("encodedPassword");
        assertThat(result.getEnabled()).isTrue();
        assertThat(result.getRoles()).hasSize(1);
        assertThat(result.getRoles()).contains(userRole);

        verify(userRepository).existsByUsername(username);
        verify(roleRepository).findByName("ROLE_USER");
        verify(passwordEncoder).encode(password);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should create user with multiple roles")
    void createUser_ShouldCreateUser_WithMultipleRoles() {
        // Arrange
        String username = "adminuser";
        String password = "adminpass";
        Set<String> roleNames = Set.of("ROLE_USER", "ROLE_ADMIN");

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
        when(passwordEncoder.encode(password)).thenReturn("encodedAdminPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = userService.createUser(username, password, roleNames);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getRoles()).hasSize(2);
        assertThat(result.getRoles()).contains(userRole, adminRole);

        verify(roleRepository).findByName("ROLE_USER");
        verify(roleRepository).findByName("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Should throw exception when username already exists")
    void createUser_ShouldThrowException_WhenUsernameExists() {
        // Arrange
        String username = "existinguser";
        String password = "password";
        Set<String> roleNames = Set.of("ROLE_USER");

        when(userRepository.existsByUsername(username)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(username, password, roleNames))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already exists: " + username);

        verify(userRepository).existsByUsername(username);
        verify(roleRepository, never()).findByName(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when role not found")
    void createUser_ShouldThrowException_WhenRoleNotFound() {
        // Arrange
        String username = "newuser";
        String password = "password";
        Set<String> roleNames = Set.of("INVALID_ROLE");

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(roleRepository.findByName("INVALID_ROLE")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(username, password, roleNames))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Role not found: INVALID_ROLE");

        verify(userRepository).existsByUsername(username);
        verify(roleRepository).findByName("INVALID_ROLE");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should enable user successfully")
    void setUserEnabled_ShouldEnableUser_WhenUserExists() {
        // Arrange
        testUser.setEnabled(false);
        when(userRepository.findByUsernameWithRoles("testuser"))
                .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        userService.setUserEnabled("testuser", true);

        // Assert
        assertThat(testUser.getEnabled()).isTrue();
        verify(userRepository).findByUsernameWithRoles("testuser");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should disable user successfully")
    void setUserEnabled_ShouldDisableUser_WhenUserExists() {
        // Arrange
        when(userRepository.findByUsernameWithRoles("testuser"))
                .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        userService.setUserEnabled("testuser", false);

        // Assert
        assertThat(testUser.getEnabled()).isFalse();
        verify(userRepository).findByUsernameWithRoles("testuser");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should throw exception when setting enabled status for non-existent user")
    void setUserEnabled_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        when(userRepository.findByUsernameWithRoles("nonexistent"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.setUserEnabled("nonexistent", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found: nonexistent");

        verify(userRepository).findByUsernameWithRoles("nonexistent");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should add role to user successfully")
    void addRoleToUser_ShouldAddRole_WhenUserAndRoleExist() {
        // Arrange
        when(userRepository.findByUsernameWithRoles("testuser"))
                .thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("ROLE_ADMIN"))
                .thenReturn(Optional.of(adminRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        userService.addRoleToUser("testuser", "ROLE_ADMIN");

        // Assert
        assertThat(testUser.getRoles()).hasSize(2);
        assertThat(testUser.getRoles()).contains(userRole, adminRole);
        verify(userRepository).findByUsernameWithRoles("testuser");
        verify(roleRepository).findByName("ROLE_ADMIN");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should throw exception when adding role to non-existent user")
    void addRoleToUser_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        when(userRepository.findByUsernameWithRoles("nonexistent"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.addRoleToUser("nonexistent", "ROLE_ADMIN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found: nonexistent");

        verify(userRepository).findByUsernameWithRoles("nonexistent");
        verify(roleRepository, never()).findByName(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when adding non-existent role to user")
    void addRoleToUser_ShouldThrowException_WhenRoleNotFound() {
        // Arrange
        when(userRepository.findByUsernameWithRoles("testuser"))
                .thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("INVALID_ROLE"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.addRoleToUser("testuser", "INVALID_ROLE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Role not found: INVALID_ROLE");

        verify(userRepository).findByUsernameWithRoles("testuser");
        verify(roleRepository).findByName("INVALID_ROLE");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should remove role from user successfully")
    void removeRoleFromUser_ShouldRemoveRole_WhenUserAndRoleExist() {
        // Arrange
        testUser.getRoles().add(adminRole);
        when(userRepository.findByUsernameWithRoles("testuser"))
                .thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("ROLE_ADMIN"))
                .thenReturn(Optional.of(adminRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        userService.removeRoleFromUser("testuser", "ROLE_ADMIN");

        // Assert
        assertThat(testUser.getRoles()).hasSize(1);
        assertThat(testUser.getRoles()).contains(userRole);
        assertThat(testUser.getRoles()).doesNotContain(adminRole);
        verify(userRepository).findByUsernameWithRoles("testuser");
        verify(roleRepository).findByName("ROLE_ADMIN");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should throw exception when removing role from non-existent user")
    void removeRoleFromUser_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        when(userRepository.findByUsernameWithRoles("nonexistent"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.removeRoleFromUser("nonexistent", "ROLE_USER"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found: nonexistent");

        verify(userRepository).findByUsernameWithRoles("nonexistent");
        verify(roleRepository, never()).findByName(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when removing non-existent role from user")
    void removeRoleFromUser_ShouldThrowException_WhenRoleNotFound() {
        // Arrange
        when(userRepository.findByUsernameWithRoles("testuser"))
                .thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("INVALID_ROLE"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.removeRoleFromUser("testuser", "INVALID_ROLE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Role not found: INVALID_ROLE");

        verify(userRepository).findByUsernameWithRoles("testuser");
        verify(roleRepository).findByName("INVALID_ROLE");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should get all users successfully")
    void getAllUsers_ShouldReturnAllUsers() {
        // Arrange
        User user2 = User.builder()
                .id(2L)
                .username("user2")
                .password("password2")
                .enabled(true)
                .roles(new HashSet<>())
                .build();

        List<User> users = List.of(testUser, user2);
        when(userRepository.findAll()).thenReturn(users);

        // Act
        Iterable<User> result = userService.getAllUsers();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).contains(testUser, user2);
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no users exist")
    void getAllUsers_ShouldReturnEmptyList_WhenNoUsers() {
        // Arrange
        when(userRepository.findAll()).thenReturn(new ArrayList<>());

        // Act
        Iterable<User> result = userService.getAllUsers();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("Should handle user with no roles when adding role")
    void addRoleToUser_ShouldWork_WhenUserHasNoRoles() {
        // Arrange
        User userWithoutRoles = User.builder()
                .id(3L)
                .username("noroles")
                .password("password")
                .enabled(true)
                .roles(new HashSet<>())
                .build();

        when(userRepository.findByUsernameWithRoles("noroles"))
                .thenReturn(Optional.of(userWithoutRoles));
        when(roleRepository.findByName("ROLE_USER"))
                .thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenReturn(userWithoutRoles);

        // Act
        userService.addRoleToUser("noroles", "ROLE_USER");

        // Assert
        assertThat(userWithoutRoles.getRoles()).hasSize(1);
        assertThat(userWithoutRoles.getRoles()).contains(userRole);
        verify(userRepository).save(userWithoutRoles);
    }

    @Test
    @DisplayName("Should not duplicate role when adding existing role")
    void addRoleToUser_ShouldNotDuplicate_WhenRoleAlreadyExists() {
        // Arrange
        when(userRepository.findByUsernameWithRoles("testuser"))
                .thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("ROLE_USER"))
                .thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        userService.addRoleToUser("testuser", "ROLE_USER");

        // Assert
        // Since Set automatically handles duplicates, the size should remain 1
        assertThat(testUser.getRoles()).hasSize(1);
        assertThat(testUser.getRoles()).contains(userRole);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should create user with empty roles set")
    void createUser_ShouldWork_WithEmptyRoles() {
        // Arrange
        String username = "minimaluser";
        String password = "password";
        Set<String> roleNames = new HashSet<>();

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("encodedMinimal");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = userService.createUser(username, password, roleNames);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getRoles()).isEmpty();
        verify(roleRepository, never()).findByName(anyString());
    }

    @Test
    @DisplayName("Should encode password when creating user")
    void createUser_ShouldEncodePassword() {
        // Arrange
        String username = "secureuser";
        String plainPassword = "PlainText123!";
        String encodedPassword = "$2a$12$encoded.password.hash";
        Set<String> roleNames = Set.of("ROLE_USER");

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(plainPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = userService.createUser(username, plainPassword, roleNames);

        // Assert
        assertThat(result.getPassword()).isEqualTo(encodedPassword);
        assertThat(result.getPassword()).isNotEqualTo(plainPassword);
        verify(passwordEncoder).encode(plainPassword);
    }
}
