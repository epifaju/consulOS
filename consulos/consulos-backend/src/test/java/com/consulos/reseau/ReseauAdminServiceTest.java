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
class ReseauAdminServiceTest {

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
    void listMembres_shouldMapUsers() {
        when(membreRepository.existsByReseauIdAndUserId(reseauId, userId)).thenReturn(true);
        when(reseauRepository.findById(reseauId)).thenReturn(Optional.of(reseau));

        UUID membreUserId = UUID.randomUUID();
        ReseauMembreEntity membre = new ReseauMembreEntity();
        membre.setUserId(membreUserId);
        membre.setRoleReseau("VIEWER");
        membre.setOrganisationId(orgId);
        when(membreAdminRepository.findByReseauIdOrderByCreatedAtAsc(reseauId)).thenReturn(List.of(membre));

        UserEntity user = new UserEntity();
        user.setId(membreUserId);
        user.setEmail("agent@consulos.local");
        user.setNom("Diallo");
        user.setPrenom("Fatou");
        when(userLookupRepository.findByIdIn(List.of(membreUserId))).thenReturn(List.of(user));

        List<ReseauMembreDTO> result = reseauAdminService.listMembres(reseauId, userId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().email()).isEqualTo("agent@consulos.local");
        assertThat(result.getFirst().roleReseau()).isEqualTo("VIEWER");
    }

    @Test
    void attachPoste_whenNotAdmin_shouldThrow() {
        ReseauMembreEntity membre = new ReseauMembreEntity();
        membre.setRoleReseau("VIEWER");
        when(membreRepository.findByReseauIdAndUserId(reseauId, userId)).thenReturn(Optional.of(membre));

        assertThatThrownBy(() -> reseauAdminService.attachPoste(
                reseauId, new ReseauAttachPosteRequestDTO("GNB-FR"), userId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("RESEAU_ADMIN_REQUIRED");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });

        verify(posteAdminRepository, never()).save(any());
    }

    @Test
    void attachPoste_whenInactiveOrganisation_shouldThrow() {
        stubAdmin();
        when(reseauRepository.findById(reseauId)).thenReturn(Optional.of(reseau));

        OrganisationEntity org = new OrganisationEntity();
        org.setId(orgId);
        org.setCode("GNB-FR");
        org.setActif(false);
        when(organisationLookupRepository.findByCode("GNB-FR")).thenReturn(Optional.of(org));

        assertThatThrownBy(() -> reseauAdminService.attachPoste(
                reseauId, new ReseauAttachPosteRequestDTO("gnb-fr"), userId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("ORGANISATION_INACTIVE"));
    }

    @Test
    void attachPoste_whenAlreadyAttached_shouldThrowConflict() {
        stubAdmin();
        when(reseauRepository.findById(reseauId)).thenReturn(Optional.of(reseau));

        OrganisationEntity org = activeOrg("GNB-FR");
        when(organisationLookupRepository.findByCode("GNB-FR")).thenReturn(Optional.of(org));
        when(posteAdminRepository.existsByReseauIdAndOrganisationId(reseauId, orgId)).thenReturn(true);

        assertThatThrownBy(() -> reseauAdminService.attachPoste(
                reseauId, new ReseauAttachPosteRequestDTO("GNB-FR"), userId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("POSTE_ALREADY_ATTACHED");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                });
    }

    @Test
    void attachPoste_whenValid_shouldSaveAndReturnDto() {
        stubAdmin();
        when(reseauRepository.findById(reseauId)).thenReturn(Optional.of(reseau));

        OrganisationEntity org = activeOrg("GNB-FR");
        org.setNom("Consulat Paris");
        org.setPaysCode("FR");
        when(organisationLookupRepository.findByCode("GNB-FR")).thenReturn(Optional.of(org));
        when(posteAdminRepository.existsByReseauIdAndOrganisationId(reseauId, orgId)).thenReturn(false);
        when(posteAdminRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReseauPosteDTO dto = reseauAdminService.attachPoste(
                reseauId, new ReseauAttachPosteRequestDTO("  gnb-fr  "), userId);

        assertThat(dto.organisationId()).isEqualTo(orgId);
        assertThat(dto.code()).isEqualTo("GNB-FR");
        ArgumentCaptor<ReseauPosteEntity> captor = ArgumentCaptor.forClass(ReseauPosteEntity.class);
        verify(posteAdminRepository).save(captor.capture());
        assertThat(captor.getValue().getOrganisationId()).isEqualTo(orgId);
        assertThat(captor.getValue().getReseauId()).isEqualTo(reseauId);
    }

    @Test
    void detachPoste_whenNotAttached_shouldThrow() {
        stubAdmin();
        when(reseauRepository.findById(reseauId)).thenReturn(Optional.of(reseau));
        when(posteAdminRepository.findByReseauIdAndOrganisationId(reseauId, orgId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reseauAdminService.detachPoste(reseauId, orgId, userId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("POSTE_NOT_ATTACHED"));
    }

    @Test
    void detachPoste_whenAttached_shouldDelete() {
        stubAdmin();
        when(reseauRepository.findById(reseauId)).thenReturn(Optional.of(reseau));
        ReseauPosteEntity poste = new ReseauPosteEntity();
        poste.setReseauId(reseauId);
        poste.setOrganisationId(orgId);
        when(posteAdminRepository.findByReseauIdAndOrganisationId(reseauId, orgId)).thenReturn(Optional.of(poste));

        reseauAdminService.detachPoste(reseauId, orgId, userId);

        verify(posteAdminRepository).delete(poste);
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
