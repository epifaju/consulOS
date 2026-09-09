package com.consulos.administration;

import com.consulos.administration.dto.UserAdminDTO;
import com.consulos.administration.dto.UserCreateDTO;
import com.consulos.administration.dto.UserCreateResponseDTO;
import com.consulos.administration.dto.UserUpdateDTO;
import com.consulos.auth.entity.RoleEntity;
import com.consulos.auth.entity.UserEntity;
import com.consulos.auth.repository.UserRepository;
import com.consulos.common.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private AdminUserDao adminUserDao;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AdministrationMailService mailService;

    @InjectMocks
    private AdminUserService adminUserService;

    private UUID organisationId;
    private UUID otherOrganisationId;
    private UUID roleId;
    private UUID userId;
    private UUID currentUserId;
    private RoleEntity role;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        otherOrganisationId = UUID.randomUUID();
        roleId = UUID.randomUUID();
        userId = UUID.randomUUID();
        currentUserId = UUID.randomUUID();

        role = new RoleEntity();
        role.setId(roleId);
        role.setCode("ROLE_AGENT");
        role.setLibelle("Agent");

        user = new UserEntity();
        user.setId(userId);
        user.setOrganisationId(organisationId);
        user.setEmail("agent@consulos.local");
        user.setNom("Camara");
        user.setPrenom("Aissatou");
        user.setRole(role);
        user.setActif(true);
    }

    @Test
    void list_shouldReturnPagedUsersForOrganisation() {
        when(adminUserDao.findByOrganisation(eq(organisationId), any()))
                .thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 20), 1));

        var page = adminUserService.list(organisationId, PageRequest.of(0, 20));

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().getFirst().email()).isEqualTo("agent@consulos.local");
        assertThat(page.content().getFirst().roleCode()).isEqualTo("ROLE_AGENT");
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void findById_whenOtherOrganisation_shouldThrowNotFound() {
        when(adminUserDao.findByIdAndOrganisation(userId, otherOrganisationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.findById(userId, otherOrganisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("USER_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    void create_shouldPersistUserEncodePasswordAndSendWelcomeMail() {
        UserCreateDTO dto = new UserCreateDTO("Agent@Consulos.local", "Diallo", "Fatou", roleId, true);
        when(adminUserDao.existsByEmail(dto.email())).thenReturn(false);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-temp");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserCreateResponseDTO result = adminUserService.create(dto, organisationId, currentUserId);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        UserEntity saved = captor.getValue();

        assertThat(saved.getEmail()).isEqualTo("agent@consulos.local");
        assertThat(saved.getOrganisationId()).isEqualTo(organisationId);
        assertThat(saved.getRole()).isEqualTo(role);
        assertThat(saved.getPasswordHash()).isEqualTo("encoded-temp");
        assertThat(saved.getActif()).isTrue();
        assertThat(saved.getCreatedBy()).isEqualTo(currentUserId);
        assertThat(result.temporaryPassword()).hasSize(12);
        verify(mailService).sendWelcomeEmail(eq("agent@consulos.local"), eq(result.temporaryPassword()));
        assertThat(result.user().nom()).isEqualTo("Diallo");
    }

    @Test
    void create_whenEmailExists_shouldThrowConflict() {
        UserCreateDTO dto = new UserCreateDTO("agent@consulos.local", "Diallo", "Fatou", roleId, true);
        when(adminUserDao.existsByEmail(dto.email())).thenReturn(true);

        assertThatThrownBy(() -> adminUserService.create(dto, organisationId, currentUserId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("EMAIL_EXISTS");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                });

        verify(userRepository, never()).save(any());
    }

    @Test
    void create_whenRoleMissing_shouldThrowNotFound() {
        UserCreateDTO dto = new UserCreateDTO("new@consulos.local", "Diallo", "Fatou", roleId, null);
        when(adminUserDao.existsByEmail(dto.email())).thenReturn(false);
        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.create(dto, organisationId, currentUserId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("ROLE_NOT_FOUND"));
    }

    @Test
    void update_shouldChangeEmailRoleAndActif() {
        UUID newRoleId = UUID.randomUUID();
        RoleEntity newRole = new RoleEntity();
        newRole.setId(newRoleId);
        newRole.setCode("ROLE_CAISSIER");
        newRole.setLibelle("Caissier");

        when(adminUserDao.findByIdAndOrganisation(userId, organisationId)).thenReturn(Optional.of(user));
        when(adminUserDao.existsByEmail("Nouveau@Consulos.local")).thenReturn(false);
        when(roleRepository.findById(newRoleId)).thenReturn(Optional.of(newRole));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserAdminDTO result = adminUserService.update(
                userId,
                new UserUpdateDTO("Nouveau@Consulos.local", "Barry", "Mamadou", newRoleId, false),
                organisationId
        );

        assertThat(user.getEmail()).isEqualTo("nouveau@consulos.local");
        assertThat(user.getNom()).isEqualTo("Barry");
        assertThat(user.getPrenom()).isEqualTo("Mamadou");
        assertThat(user.getRole()).isEqualTo(newRole);
        assertThat(user.getActif()).isFalse();
        assertThat(result.roleCode()).isEqualTo("ROLE_CAISSIER");
    }

    @Test
    void update_whenEmailTaken_shouldThrowConflict() {
        when(adminUserDao.findByIdAndOrganisation(userId, organisationId)).thenReturn(Optional.of(user));
        when(adminUserDao.existsByEmail("taken@consulos.local")).thenReturn(true);

        assertThatThrownBy(() -> adminUserService.update(
                userId,
                new UserUpdateDTO("taken@consulos.local", null, null, null, null),
                organisationId
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("EMAIL_EXISTS"));
    }

    @Test
    void toggleActif_shouldFlipActiveFlag() {
        when(adminUserDao.findByIdAndOrganisation(userId, organisationId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserAdminDTO result = adminUserService.toggleActif(userId, organisationId);

        assertThat(user.getActif()).isFalse();
        assertThat(result.actif()).isFalse();
    }

    @Test
    void resetPassword_shouldUpdateHashAndSendMail() {
        when(adminUserDao.findByIdAndOrganisation(userId, organisationId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-reset");

        String temp = adminUserService.resetPassword(userId, organisationId);

        assertThat(temp).hasSize(12);
        verify(userRepository).updatePassword(eq(userId), eq("encoded-reset"), any());
        verify(mailService).sendWelcomeEmail(user.getEmail(), temp);
    }

    @Test
    void delete_shouldSoftDeactivateUser() {
        when(adminUserDao.findByIdAndOrganisation(userId, organisationId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adminUserService.delete(userId, organisationId, currentUserId);

        assertThat(user.getActif()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void delete_whenSelf_shouldThrowBadRequest() {
        assertThatThrownBy(() -> adminUserService.delete(currentUserId, organisationId, currentUserId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("DELETE_SELF");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });

        verify(adminUserDao, never()).findByIdAndOrganisation(any(), any());
    }
}
