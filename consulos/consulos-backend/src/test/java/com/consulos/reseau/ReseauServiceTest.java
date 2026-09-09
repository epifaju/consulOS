package com.consulos.reseau;

import com.consulos.common.BusinessException;
import com.consulos.reseau.dto.ReseauMeDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReseauServiceTest {

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
    private UUID orgC;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        reseauId = UUID.randomUUID();
        orgA = UUID.randomUUID();
        orgB = UUID.randomUUID();
        orgC = UUID.randomUUID();
    }

    @Test
    void resolveAllowedOrganisationIds_whenMember_shouldReturnPostesOfReseau() {
        when(membreRepository.existsByReseauIdAndUserId(reseauId, userId)).thenReturn(true);
        when(posteRepository.findByReseauId(reseauId)).thenReturn(List.of(
                poste(reseauId, orgA),
                poste(reseauId, orgB)
        ));

        Set<UUID> allowed = reseauService.resolveAllowedOrganisationIds(reseauId, userId);

        assertThat(allowed).containsExactly(orgA, orgB);
    }

    @Test
    void resolveAllowedOrganisationIds_whenNotMember_shouldThrowForbidden() {
        when(membreRepository.existsByReseauIdAndUserId(reseauId, userId)).thenReturn(false);

        assertThatThrownBy(() -> reseauService.resolveAllowedOrganisationIds(reseauId, userId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("RESEAU_FORBIDDEN");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });

        verify(posteRepository, never()).findByReseauId(reseauId);
    }

    @Test
    void resolveAllAllowedOrganisationIds_whenNoMembership_shouldReturnEmpty() {
        when(membreRepository.findByUserId(userId)).thenReturn(List.of());

        assertThat(reseauService.resolveAllAllowedOrganisationIds(userId)).isEmpty();
        verify(posteRepository, never()).findByReseauIdIn(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void resolveAllAllowedOrganisationIds_shouldUnionOrganisationsAcrossReseaux() {
        UUID reseau2 = UUID.randomUUID();
        ReseauMembreEntity m1 = membership(reseauId, userId);
        ReseauMembreEntity m2 = membership(reseau2, userId);
        when(membreRepository.findByUserId(userId)).thenReturn(List.of(m1, m2));
        when(posteRepository.findByReseauIdIn(List.of(reseauId, reseau2))).thenReturn(List.of(
                poste(reseauId, orgA),
                poste(reseauId, orgB),
                poste(reseau2, orgB),
                poste(reseau2, orgC)
        ));

        Set<UUID> allowed = reseauService.resolveAllAllowedOrganisationIds(userId);

        assertThat(allowed).containsExactly(orgA, orgB, orgC);
    }

    @Test
    void getMyReseaux_whenNoMembership_shouldReturnEmptyList() {
        when(membreRepository.findByUserId(userId)).thenReturn(List.of());

        ReseauMeDTO result = reseauService.getMyReseaux(userId);

        assertThat(result.reseaux()).isEmpty();
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
}
