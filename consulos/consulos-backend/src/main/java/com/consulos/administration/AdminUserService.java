package com.consulos.administration;

import com.consulos.administration.dto.UserAdminDTO;
import com.consulos.administration.dto.UserCreateDTO;
import com.consulos.administration.dto.UserCreateResponseDTO;
import com.consulos.administration.dto.UserUpdateDTO;
import com.consulos.auth.entity.RoleEntity;
import com.consulos.auth.entity.UserEntity;
import com.consulos.auth.repository.UserRepository;
import com.consulos.common.BusinessException;
import com.consulos.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$";

    private final AdminUserDao adminUserDao;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdministrationMailService mailService;

    @Transactional(readOnly = true)
    public PageResponse<UserAdminDTO> list(UUID organisationId, Pageable pageable) {
        Page<UserEntity> page = adminUserDao.findByOrganisation(organisationId, pageable);
        return new PageResponse<>(
                page.getContent().stream().map(this::toDto).toList(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize(),
                page.isFirst(),
                page.isLast()
        );
    }

    @Transactional(readOnly = true)
    public UserAdminDTO findById(UUID id, UUID organisationId) {
        return toDto(getUser(id, organisationId));
    }

    @Transactional
    public UserCreateResponseDTO create(UserCreateDTO dto, UUID organisationId, UUID createdBy) {
        if (adminUserDao.existsByEmail(dto.email())) {
            throw new BusinessException("EMAIL_EXISTS", "Cet email est déjà utilisé", HttpStatus.CONFLICT);
        }
        RoleEntity role = roleRepository.findById(dto.roleId())
                .orElseThrow(() -> new BusinessException("ROLE_NOT_FOUND", "Rôle introuvable", HttpStatus.NOT_FOUND));

        String tempPassword = generateTemporaryPassword();
        OffsetDateTime now = OffsetDateTime.now();

        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setOrganisationId(organisationId);
        user.setEmail(dto.email().toLowerCase());
        user.setNom(dto.nom());
        user.setPrenom(dto.prenom());
        user.setRole(role);
        user.setActif(dto.actif() != null ? dto.actif() : true);
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setCreatedBy(createdBy);

        UserEntity saved = userRepository.save(user);
        mailService.sendWelcomeEmail(saved.getEmail(), tempPassword);
        return new UserCreateResponseDTO(toDto(saved), tempPassword);
    }

    @Transactional
    public UserAdminDTO update(UUID id, UserUpdateDTO dto, UUID organisationId) {
        UserEntity user = getUser(id, organisationId);
        if (dto.email() != null && !dto.email().equalsIgnoreCase(user.getEmail())) {
            if (adminUserDao.existsByEmail(dto.email())) {
                throw new BusinessException("EMAIL_EXISTS", "Cet email est déjà utilisé", HttpStatus.CONFLICT);
            }
            user.setEmail(dto.email().toLowerCase());
        }
        if (dto.nom() != null) {
            user.setNom(dto.nom());
        }
        if (dto.prenom() != null) {
            user.setPrenom(dto.prenom());
        }
        if (dto.roleId() != null) {
            RoleEntity role = roleRepository.findById(dto.roleId())
                    .orElseThrow(() -> new BusinessException("ROLE_NOT_FOUND", "Rôle introuvable", HttpStatus.NOT_FOUND));
            user.setRole(role);
        }
        if (dto.actif() != null) {
            user.setActif(dto.actif());
        }
        user.setUpdatedAt(OffsetDateTime.now());
        return toDto(userRepository.save(user));
    }

    @Transactional
    public UserAdminDTO toggleActif(UUID id, UUID organisationId) {
        UserEntity user = getUser(id, organisationId);
        user.setActif(!Boolean.TRUE.equals(user.getActif()));
        user.setUpdatedAt(OffsetDateTime.now());
        return toDto(userRepository.save(user));
    }

    @Transactional
    public String resetPassword(UUID id, UUID organisationId) {
        UserEntity user = getUser(id, organisationId);
        String tempPassword = generateTemporaryPassword();
        userRepository.updatePassword(user.getId(), passwordEncoder.encode(tempPassword), OffsetDateTime.now());
        mailService.sendWelcomeEmail(user.getEmail(), tempPassword);
        return tempPassword;
    }

    @Transactional
    public void delete(UUID id, UUID organisationId, UUID currentUserId) {
        if (id.equals(currentUserId)) {
            throw new BusinessException("DELETE_SELF", "Impossible de supprimer votre propre compte", HttpStatus.BAD_REQUEST);
        }
        UserEntity user = getUser(id, organisationId);
        user.setActif(false);
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);
    }

    private UserEntity getUser(UUID id, UUID organisationId) {
        return adminUserDao.findByIdAndOrganisation(id, organisationId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Utilisateur introuvable", HttpStatus.NOT_FOUND));
    }

    private UserAdminDTO toDto(UserEntity user) {
        return new UserAdminDTO(
                user.getId(),
                user.getEmail(),
                user.getNom(),
                user.getPrenom(),
                user.getRole().getCode(),
                user.getRole().getLibelle(),
                user.getActif(),
                user.getDerniereConnexion(),
                user.getCreatedAt()
        );
    }

    private String generateTemporaryPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(PASSWORD_CHARS.charAt(random.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}
