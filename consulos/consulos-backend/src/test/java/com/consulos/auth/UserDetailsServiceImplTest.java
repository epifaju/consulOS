package com.consulos.auth;

import com.consulos.auth.entity.RoleEntity;
import com.consulos.auth.entity.UserEntity;
import com.consulos.auth.repository.UserRepository;
import com.consulos.common.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private UserEntity activeUser;
    private UUID userId;
    private UUID organisationId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        organisationId = UUID.randomUUID();

        RoleEntity role = new RoleEntity();
        role.setCode("ROLE_AGENT");

        activeUser = new UserEntity();
        activeUser.setId(userId);
        activeUser.setEmail("agent@consulos.local");
        activeUser.setPasswordHash("hash");
        activeUser.setOrganisationId(organisationId);
        activeUser.setRole(role);
        activeUser.setActif(true);
    }

    @Test
    void loadUserByUsername_whenFound_shouldMapDetails() {
        when(userRepository.findByEmail("agent@consulos.local")).thenReturn(Optional.of(activeUser));

        ConsulosUserDetails details = (ConsulosUserDetails) userDetailsService.loadUserByUsername("agent@consulos.local");

        assertThat(details.getUserId()).isEqualTo(userId);
        assertThat(details.getUsername()).isEqualTo("agent@consulos.local");
        assertThat(details.getOrganisationId()).isEqualTo(organisationId);
        assertThat(details.getAuthorities()).extracting(Object::toString).contains("ROLE_AGENT");
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    void loadUserByUsername_whenMissing_shouldThrowUsernameNotFound() {
        when(userRepository.findByEmail("missing@consulos.local")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("missing@consulos.local"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("missing@consulos.local");
    }

    @Test
    void loadUserById_whenFoundInactive_shouldMapActifFalse() {
        activeUser.setActif(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        ConsulosUserDetails details = userDetailsService.loadUserById(userId);

        assertThat(details.getUserId()).isEqualTo(userId);
        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    void loadUserById_whenMissing_shouldThrowUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserById(userId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("USER_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    void loadUserById_whenActifNull_shouldTreatAsDisabled() {
        activeUser.setActif(null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        ConsulosUserDetails details = userDetailsService.loadUserById(userId);

        assertThat(details.isEnabled()).isFalse();
    }
}
