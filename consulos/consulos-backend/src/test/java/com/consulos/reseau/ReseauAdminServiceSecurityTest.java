package com.consulos.reseau;

import com.consulos.auth.entity.OrganisationEntity;
import com.consulos.auth.entity.UserEntity;
import com.consulos.common.BusinessException;
import com.consulos.reseau.dto.ReseauAttachPosteRequestDTO;
import com.consulos.reseau.dto.ReseauMembreDTO;
import com.consulos.reseau.dto.ReseauPosteDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReseauAdminServiceSecurityTest {

    @Mock
    private ReseauRepository reseauRepository;
    @Mock
    private ReseauMembreRepository membreRepository;
    @Mock
    private ReseauMembreAdminRepository membreAdminRepository;
    @Mock
    private ReseauPosteAdminRepository posteAdminRepository;
    @Mock
    private ReseauOrganisationLookupRepository organisationLookupRepository;
    @Mock
    private ReseauUserLookupRepository userLookupRepository;

    @InjectMocks
    private ReseauAdminService reseauAdminService;

    private UUID reseauId;
    private UUID userId;
    private UUID orgId;
    private ReseauEntity reseau;

    @BeforeEach
    void setUp() {
        reseauId = UUID.randomUUID();
        userId = UUID.randomUUID();
        orgId = UUID.randomUUID();
        reseau = new ReseauEntity();
        reseau.setId(reseauId);
        reseau.setCode("EUR");
        reseau.setActif(true);
    }

    @Test
    void listMembres_whenNotMember_shouldThrowReseauForbidden() {
        when(membreRepository.existsByReseauIdAndUserId(reseauId, userId)).thenReturn(false);

        assertThatThrownBy(() -> reseauAdminService.listMembres(reseauId, userId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("RESEAU_FORBIDDEN");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });

        verify(membreAdminRepository, never()).findByReseauIdOrderByCreatedAtAsc(any());
    }

    @Test
    void listMembres_whenReseauInactive_shouldThrowNotFound() {
        when(membreRepository.existsByReseauIdAndUserId(reseauId, userId)).thenReturn(true);
        reseau.setActif(false);
        when(reseauRepository.findById(reseauId)).thenReturn(Optional.of(reseau));

        assertThatThrownBy(() -> reseauAdminService.listMembres(reseauId, userId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("RESEAU_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    void listMembres_whenUserLookupMissing_shouldSkipOrphan() {
        when(membreRepository.existsByReseauIdAndUserId(reseauId, userId)).thenReturn(true);
        when(reseauRepository.findById(reseauId)).thenReturn(Optional.of(reseau));

        UUID knownUserId = UUID.randomUUID();
        UUID orphanUserId = UUID.randomUUID();

        ReseauMembreEntity known = new ReseauMembreEntity();
        known.setUserId(knownUserId);
        known.setRoleReseau("VIEWER");
        known.setOrganisationId(orgId);

        ReseauMembreEntity orphan = new ReseauMembreEntity();
        orphan.setUserId(orphanUserId);
        orphan.setRoleReseau("VIEWER");
        orphan.setOrganisationId(orgId);

        when(membreAdminRepository.findByReseauIdOrderByCreatedAtAsc(reseauId))
                .thenReturn(List.of(known, orphan));

        UserEntity user = new UserEntity();
        user.setId(knownUserId);
        user.setEmail("known@consulos.local");
        user.setNom("Known");
        user.setPrenom("User");
        when(userLookupRepository.findByIdIn(List.of(knownUserId, orphanUserId)))
                .thenReturn(List.of(user));

        List<ReseauMembreDTO> result = reseauAdminService.listMembres(reseauId, userId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().email()).isEqualTo("known@consulos.local");
        assertThat(result.getFirst().userId()).isEqualTo(knownUserId);
    }

    @Test
    void attachPoste_whenOrganisationCodeUnknown_shouldThrowNotFound() {
        stubAdmin();
        when(reseauRepository.findById(reseauId)).thenReturn(Optional.of(reseau));
        when(organisationLookupRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reseauAdminService.attachPoste(
                reseauId, new ReseauAttachPosteRequestDTO("unknown"), userId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("ORGANISATION_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verify(posteAdminRepository, never()).save(any());
    }

    @Test
    void attachPoste_whenNotMember_shouldThrowForbidden() {
        when(membreRepository.findByReseauIdAndUserId(reseauId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reseauAdminService.attachPoste(
                reseauId, new ReseauAttachPosteRequestDTO("GNB-FR"), userId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("RESEAU_FORBIDDEN");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });

        verify(posteAdminRepository, never()).save(any());
    }

    @Test
    void attachPoste_whenReseauInactive_shouldThrowNotFound() {
        stubAdmin();
        reseau.setActif(false);
        when(reseauRepository.findById(reseauId)).thenReturn(Optional.of(reseau));

        assertThatThrownBy(() -> reseauAdminService.attachPoste(
                reseauId, new ReseauAttachPosteRequestDTO("GNB-FR"), userId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("RESEAU_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    void detachPoste_whenNotAdmin_shouldThrowAdminRequired() {
        ReseauMembreEntity membre = new ReseauMembreEntity();
        membre.setRoleReseau("VIEWER");
        when(membreRepository.findByReseauIdAndUserId(reseauId, userId)).thenReturn(Optional.of(membre));

        assertThatThrownBy(() -> reseauAdminService.detachPoste(reseauId, orgId, userId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("RESEAU_ADMIN_REQUIRED");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });

        verify(posteAdminRepository, never()).delete(any());
    }

    @Test
    void attachPoste_whenRoleAdminIgnoreCase_shouldPass() {
        ReseauMembreEntity admin = new ReseauMembreEntity();
        admin.setRoleReseau("admin");
        when(membreRepository.findByReseauIdAndUserId(reseauId, userId)).thenReturn(Optional.of(admin));
        when(reseauRepository.findById(reseauId)).thenReturn(Optional.of(reseau));

        OrganisationEntity org = activeOrg("GNB-FR");
        org.setNom("Consulat Paris");
        org.setPaysCode("FR");
        when(organisationLookupRepository.findByCode("GNB-FR")).thenReturn(Optional.of(org));
        when(posteAdminRepository.existsByReseauIdAndOrganisationId(reseauId, orgId)).thenReturn(false);
        when(posteAdminRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReseauPosteDTO dto = reseauAdminService.attachPoste(
                reseauId, new ReseauAttachPosteRequestDTO("GNB-FR"), userId);

        assertThat(dto.organisationId()).isEqualTo(orgId);
        assertThat(dto.code()).isEqualTo("GNB-FR");
        ArgumentCaptor<ReseauPosteEntity> captor = ArgumentCaptor.forClass(ReseauPosteEntity.class);
        verify(posteAdminRepository).save(captor.capture());
        assertThat(captor.getValue().getOrganisationId()).isEqualTo(orgId);
        assertThat(captor.getValue().getReseauId()).isEqualTo(reseauId);
    }

    private void stubAdmin() {
        ReseauMembreEntity admin = new ReseauMembreEntity();
        admin.setRoleReseau(ReseauAdminService.ROLE_ADMIN);
        when(membreRepository.findByReseauIdAndUserId(reseauId, userId)).thenReturn(Optional.of(admin));
    }

    private OrganisationEntity activeOrg(String code) {
        OrganisationEntity org = new OrganisationEntity();
        org.setId(orgId);
        org.setCode(code);
        org.setActif(true);
        return org;
    }
}
