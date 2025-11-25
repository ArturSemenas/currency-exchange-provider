package com.currencyexchange.provider.security;

import com.currencyexchange.provider.model.Role;
import com.currencyexchange.provider.model.User;
import com.currencyexchange.provider.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for UserDetailsServiceImpl.
 * Tests Spring Security integration for user authentication and authorization.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserDetailsServiceImpl Unit Tests")
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private User testUser;
    private Role roleUser;
    private Role roleAdmin;

    @BeforeEach
    void setUp() {
        // Create test roles
        roleUser = Role.builder()
                .id(1L)
                .name("ROLE_USER")
                .users(new HashSet<>())
                .build();

        roleAdmin = Role.builder()
                .id(2L)
                .name("ROLE_ADMIN")
                .users(new HashSet<>())
                .build();

        // Create test user with roles
        Set<Role> roles = new HashSet<>();
        roles.add(roleUser);

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .password("$2a$12$encodedPassword")
                .enabled(true)
                .roles(roles)
                .build();
    }

    // ==================== loadUserByUsername Tests ====================

    @Test
    @DisplayName("loadUserByUsername should load user with correct details")
    void loadUserByUsername_ShouldLoadUserWithCorrectDetails() {
        // Given
        when(userRepository.findByUsernameWithRoles(eq("testuser")))
                .thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
        assertThat(userDetails.getPassword()).isEqualTo("$2a$12$encodedPassword");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.getAuthorities())
                .hasSize(1)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");

        verify(userRepository).findByUsernameWithRoles(eq("testuser"));
    }

    @Test
    @DisplayName("loadUserByUsername should load user with multiple roles")
    void loadUserByUsername_ShouldLoadUserWithMultipleRoles() {
        // Given
        testUser.getRoles().add(roleAdmin);
        when(userRepository.findByUsernameWithRoles(eq("adminuser")))
                .thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("adminuser");

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getAuthorities())
                .hasSize(2)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");

        verify(userRepository).findByUsernameWithRoles(eq("adminuser"));
    }

    @Test
    @DisplayName("loadUserByUsername should load disabled user correctly")
    void loadUserByUsername_ShouldLoadDisabledUserCorrectly() {
        // Given
        testUser.setEnabled(false);
        when(userRepository.findByUsernameWithRoles(eq("disableduser")))
                .thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("disableduser");

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.isEnabled()).isFalse();
        assertThat(userDetails.getUsername()).isEqualTo("testuser");

        verify(userRepository).findByUsernameWithRoles(eq("disableduser"));
    }

    @Test
    @DisplayName("loadUserByUsername should load user with no roles")
    void loadUserByUsername_ShouldLoadUserWithNoRoles() {
        // Given
        testUser.setRoles(new HashSet<>());
        when(userRepository.findByUsernameWithRoles(eq("noroles")))
                .thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("noroles");

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getAuthorities()).isEmpty();
        assertThat(userDetails.getUsername()).isEqualTo("testuser");

        verify(userRepository).findByUsernameWithRoles(eq("noroles"));
    }

    @Test
    @DisplayName("loadUserByUsername should throw UsernameNotFoundException when user not found")
    void loadUserByUsername_ShouldThrowException_WhenUserNotFound() {
        // Given
        when(userRepository.findByUsernameWithRoles(eq("nonexistent")))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nonexistent"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found: nonexistent");

        verify(userRepository).findByUsernameWithRoles(eq("nonexistent"));
    }

    @Test
    @DisplayName("loadUserByUsername should handle null username gracefully")
    void loadUserByUsername_ShouldHandleNullUsername() {
        // Given
        when(userRepository.findByUsernameWithRoles(null))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(null))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found: null");

        verify(userRepository).findByUsernameWithRoles(null);
    }

    @Test
    @DisplayName("loadUserByUsername should handle empty username")
    void loadUserByUsername_ShouldHandleEmptyUsername() {
        // Given
        when(userRepository.findByUsernameWithRoles(eq("")))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(""))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found: ");

        verify(userRepository).findByUsernameWithRoles(eq(""));
    }

    // ==================== Authority Mapping Tests ====================

    @Test
    @DisplayName("loadUserByUsername should map roles to SimpleGrantedAuthority")
    void loadUserByUsername_ShouldMapRolesToSimpleGrantedAuthority() {
        // Given
        when(userRepository.findByUsernameWithRoles(eq("testuser")))
                .thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Then
        assertThat(userDetails.getAuthorities())
                .allMatch(auth -> auth instanceof SimpleGrantedAuthority);
    }

    @Test
    @DisplayName("loadUserByUsername should preserve exact role names")
    void loadUserByUsername_ShouldPreserveExactRoleNames() {
        // Given
        Role customRole = Role.builder()
                .id(3L)
                .name("CUSTOM_ROLE_NAME")
                .users(new HashSet<>())
                .build();
        testUser.getRoles().add(customRole);

        when(userRepository.findByUsernameWithRoles(eq("testuser")))
                .thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Then
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains("CUSTOM_ROLE_NAME", "ROLE_USER");
    }

    // ==================== UserDetails Properties Tests ====================

    @Test
    @DisplayName("loadUserByUsername should return UserDetails with accountNonExpired true")
    void loadUserByUsername_ShouldReturnAccountNonExpiredTrue() {
        // Given
        when(userRepository.findByUsernameWithRoles(eq("testuser")))
                .thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Then
        assertThat(userDetails.isAccountNonExpired()).isTrue();
    }

    @Test
    @DisplayName("loadUserByUsername should return UserDetails with accountNonLocked true")
    void loadUserByUsername_ShouldReturnAccountNonLockedTrue() {
        // Given
        when(userRepository.findByUsernameWithRoles(eq("testuser")))
                .thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Then
        assertThat(userDetails.isAccountNonLocked()).isTrue();
    }

    @Test
    @DisplayName("loadUserByUsername should return UserDetails with credentialsNonExpired true")
    void loadUserByUsername_ShouldReturnCredentialsNonExpiredTrue() {
        // Given
        when(userRepository.findByUsernameWithRoles(eq("testuser")))
                .thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Then
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("loadUserByUsername should handle username with special characters")
    void loadUserByUsername_ShouldHandleSpecialCharactersInUsername() {
        // Given
        String specialUsername = "user@example.com";
        testUser.setUsername(specialUsername);
        when(userRepository.findByUsernameWithRoles(eq(specialUsername)))
                .thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername(specialUsername);

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(specialUsername);

        verify(userRepository).findByUsernameWithRoles(eq(specialUsername));
    }

    @Test
    @DisplayName("loadUserByUsername should handle username with whitespace")
    void loadUserByUsername_ShouldHandleUsernameWithWhitespace() {
        // Given
        String usernameWithSpace = "test user";
        testUser.setUsername(usernameWithSpace);
        when(userRepository.findByUsernameWithRoles(eq(usernameWithSpace)))
                .thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername(usernameWithSpace);

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(usernameWithSpace);
    }

    @Test
    @DisplayName("loadUserByUsername should handle very long usernames")
    void loadUserByUsername_ShouldHandleLongUsernames() {
        // Given
        String longUsername = "a".repeat(50);
        testUser.setUsername(longUsername);
        when(userRepository.findByUsernameWithRoles(eq(longUsername)))
                .thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername(longUsername);

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(longUsername);
    }

    @Test
    @DisplayName("loadUserByUsername should handle case-sensitive usernames")
    void loadUserByUsername_ShouldHandleCaseSensitiveUsernames() {
        // Given
        when(userRepository.findByUsernameWithRoles(eq("TestUser")))
                .thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("TestUser");

        // Then
        assertThat(userDetails).isNotNull();
        verify(userRepository).findByUsernameWithRoles(eq("TestUser"));
    }

    @Test
    @DisplayName("loadUserByUsername should handle BCrypt encoded passwords")
    void loadUserByUsername_ShouldHandleBCryptEncodedPasswords() {
        // Given
        String bcryptPassword = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        testUser.setPassword(bcryptPassword);
        when(userRepository.findByUsernameWithRoles(eq("testuser")))
                .thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Then
        assertThat(userDetails.getPassword()).isEqualTo(bcryptPassword);
    }

    @Test
    @DisplayName("loadUserByUsername should create unique authority instances for duplicate role names")
    void loadUserByUsername_ShouldHandleDuplicateRoleNames() {
        // Given - This shouldn't happen in practice, but testing robustness
        Role duplicateRole = Role.builder()
                .id(4L)
                .name("ROLE_USER")
                .users(new HashSet<>())
                .build();
        testUser.getRoles().add(duplicateRole);

        when(userRepository.findByUsernameWithRoles(eq("testuser")))
                .thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Then
        // Set should deduplicate authorities with same name
        assertThat(userDetails.getAuthorities())
                .hasSize(1)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("loadUserByUsername should load user with enabled=true flag")
    void loadUserByUsername_ShouldLoadEnabledUser() {
        // Given
        testUser.setEnabled(true);
        when(userRepository.findByUsernameWithRoles(eq("enableduser")))
                .thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("enableduser");

        // Then
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("loadUserByUsername should handle user with single admin role")
    void loadUserByUsername_ShouldLoadUserWithSingleAdminRole() {
        // Given
        testUser.setRoles(new HashSet<>(Set.of(roleAdmin)));
        when(userRepository.findByUsernameWithRoles(eq("adminonly")))
                .thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("adminonly");

        // Then
        assertThat(userDetails.getAuthorities())
                .hasSize(1)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("loadUserByUsername should return immutable authorities collection")
    void loadUserByUsername_ShouldReturnImmutableAuthorities() {
        // Given
        when(userRepository.findByUsernameWithRoles(eq("testuser")))
                .thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Then
        assertThat(userDetails.getAuthorities()).isNotNull();
        // Spring Security UserDetails typically returns unmodifiable collections
        assertThatThrownBy(() -> userDetails.getAuthorities().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
