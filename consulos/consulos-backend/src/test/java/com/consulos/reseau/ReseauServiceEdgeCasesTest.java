package com.consulos.reseau;

import com.consulos.auth.entity.OrganisationEntity;
import com.consulos.common.BusinessException;
import com.consulos.reseau.dto.ReseauMeDTO;
import com.consulos.reseau.dto.ReseauPosteDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Écarts non couverts par ReseauServiceTest : getMyReseaux enrichi, listPostes.
 */
@ExtendWith(MockitoExtension.class)
class ReseauServiceEdgeCasesTest {

    @Mock
    private ReseauRepository reseauRepository;
    @Mock
    private ReseauMembreRepository membreRepository;
    @Mock
    private ReseauPosteRepository posteRepository;
    @Mock
    private ReseauOrganisationLookupRepository organisationLookupRepository;
    @Mock
    private ReseauProperties properties;

    @InjectMocks
    private ReseauService reseauService;

    private UUID userId;
    private UUID reseauId;
    private UUID orgA;
    private UUID orgB;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        reseauId = UUID.randomUUID();
        orgA = UUID.randomUUID();
        orgB = UUID.randomUUID();
    }

    @Test
    void getMyReseaux_rich_shouldIncludeRoleAndPostes() {
        ReseauMembreEntity membre = membership(reseauId, userId);
        membre.setRoleReseau("ADMIN_RESEAU");
        ReseauEntity reseau = reseau(reseauId, "AFRIQUE-OUEST", "Réseau Afrique de l'Ouest");
        OrganisationEntity organisationA = organisation(orgA, "GNB", "Ambassade Guinée-Bissau", "GNB");
        OrganisationEntity organisationB = organisation(orgB, "SEN", "Ambassade Sénégal", "SEN");

        when(membreRepository.findByUserId(userId)).thenReturn(List.of(membre));
        when(reseauRepository.findByIdInAndActifTrue(List.of(reseauId))).thenReturn(List.of(reseau));
        when(posteRepository.findByReseauIdIn(List.of(reseauId))).thenReturn(List.of(
                poste(reseauId, orgA),
                poste(reseauId, orgB)
        ));
        when(organisationLookupRepository.findByIdIn(anyCollection()))
                .thenReturn(List.of(organisationA, organisationB));

        ReseauMeDTO result = reseauService.getMyReseaux(userId);

        assertThat(result.reseaux()).hasSize(1);
        assertThat(result.reseaux().getFirst().code()).isEqualTo("AFRIQUE-OUEST");
        assertThat(result.reseaux().getFirst().roleReseau()).isEqualTo("ADMIN_RESEAU");
        assertThat(result.reseaux().getFirst().postes()).hasSize(2);
        assertThat(result.reseaux().getFirst().postes())
                .extracting(ReseauPosteDTO::code)
                .containsExactlyInAnyOrder("GNB", "SEN");
    }

    @Test
    void listPostes_success_shouldReturnOrganisations() {
        ReseauEntity reseau = reseau(reseauId, "AFRIQUE-OUEST", "Réseau Afrique de l'Ouest");
        OrganisationEntity organisationA = organisation(orgA, "GNB", "Ambassade Guinée-Bissau", "GNB");

        when(membreRepository.existsByReseauIdAndUserId(reseauId, userId)).thenReturn(true);
        when(reseauRepository.findById(reseauId)).thenReturn(Optional.of(reseau));
        when(posteRepository.findByReseauId(reseauId)).thenReturn(List.of(poste(reseauId, orgA)));
        when(organisationLookupRepository.findByIdIn(anyCollection())).thenReturn(List.of(organisationA));

        List<ReseauPosteDTO> postes = reseauService.listPostes(reseauId, userId);

        assertThat(postes).hasSize(1);
        assertThat(postes.getFirst().organisationId()).isEqualTo(orgA);
        assertThat(postes.getFirst().code()).isEqualTo("GNB");
        assertThat(postes.getFirst().nom()).isEqualTo("Ambassade Guinée-Bissau");
        assertThat(postes.getFirst().paysCode()).isEqualTo("GNB");
        assertThat(postes.getFirst().actif()).isTrue();
    }

    @Test
    void listPostes_whenNotMember_shouldThrowForbidden() {
        when(membreRepository.existsByReseauIdAndUserId(reseauId, userId)).thenReturn(false);

        assertThatThrownBy(() -> reseauService.listPostes(reseauId, userId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("RESEAU_FORBIDDEN");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });

        verify(reseauRepository, never()).findById(reseauId);
        verify(posteRepository, never()).findByReseauId(reseauId);
    }

    @Test
    void listPostes_whenReseauInactive_shouldThrowNotFound() {
        ReseauEntity inactive = reseau(reseauId, "INACTIF", "Réseau inactif");
        inactive.setActif(false);

        when(membreRepository.existsByReseauIdAndUserId(reseauId, userId)).thenReturn(true);
        when(reseauRepository.findById(reseauId)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> reseauService.listPostes(reseauId, userId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("RESEAU_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verify(posteRepository, never()).findByReseauId(reseauId);
    }

    private ReseauPosteEntity poste(UUID reseau, UUID organisationId) {
        ReseauPosteEntity poste = new ReseauPosteEntity();
        poste.setId(UUID.randomUUID());
        poste.setReseauId(reseau);
        poste.setOrganisationId(organisationId);
        return poste;
    }

    private ReseauMembreEntity membership(UUID reseau, UUID user) {
        ReseauMembreEntity membre = new ReseauMembreEntity();
        membre.setId(UUID.randomUUID());
        membre.setReseauId(reseau);
        membre.setUserId(user);
        membre.setRoleReseau("LECTEUR");
        return membre;
    }

    private ReseauEntity reseau(UUID id, String code, String nom) {
        ReseauEntity entity = new ReseauEntity();
        entity.setId(id);
        entity.setCode(code);
        entity.setNom(nom);
        entity.setDescription("desc");
        entity.setActif(true);
        return entity;
    }

    private OrganisationEntity organisation(UUID id, String code, String nom, String paysCode) {
        OrganisationEntity org = new OrganisationEntity();
        org.setId(id);
        org.setCode(code);
        org.setNom(nom);
        org.setPaysCode(paysCode);
        org.setActif(true);
        return org;
    }
}
