package com.consulos.dashboard.regional;

import com.consulos.auth.ConsulosUserDetails;
import com.consulos.dashboard.regional.dto.RegionalAlerteDTO;
import com.consulos.reseau.ReseauOrganisationLookupRepository;
import com.consulos.reseau.ReseauService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegionalAlertsServiceTest {

    @Mock
    private RegionalAlertsQueryRepository repository;
    @Mock
    private ReseauService reseauService;
    @Mock
    private ReseauOrganisationLookupRepository organisationLookupRepository;

    @InjectMocks
    private RegionalAlertsService regionalAlertsService;

    private UUID userId;
    private UUID reseauId;
    private UUID orgA;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        reseauId = UUID.randomUUID();
        orgA = UUID.randomUUID();
        ConsulosUserDetails details = new ConsulosUserDetails(
                userId, "chef@consulos.local", "hash", orgA, "ROLE_ADMIN", true
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAlertes_whenEmptyScope_shouldReturnEmpty() {
        when(reseauService.resolveAllowedOrganisationIds(reseauId, userId)).thenReturn(Set.of());

        assertThat(regionalAlertsService.getAlertes(reseauId)).isEmpty();
    }

    @Test
    void getAlertes_shouldIncludeCriticalEcheancesAndBlocked() {
        UUID dossierBloque = UUID.randomUUID();
        UUID dossierEcheance = UUID.randomUUID();
        when(reseauService.resolveAllowedOrganisationIds(reseauId, userId)).thenReturn(Set.of(orgA));
        when(repository.dossiersBloquesEnRevue(Set.of(orgA))).thenReturn(List.of(dossierBloque));
        when(repository.posteCodesForDossierIds(List.of(dossierBloque))).thenReturn(List.of("GNB-FR"));
        when(repository.dossiersEcheanceDepassee(Set.of(orgA))).thenReturn(List.of(dossierEcheance));
        when(repository.posteCodesForDossierIds(List.of(dossierEcheance))).thenReturn(List.of("GNB-FR"));
        when(repository.agentsSurcharges(Set.of(orgA))).thenReturn(List.of());
        when(repository.postesCaisseSilencieuse(eq(Set.of(orgA)), any())).thenReturn(List.of());

        List<RegionalAlerteDTO> alertes = regionalAlertsService.getAlertes(reseauId);

        assertThat(alertes).extracting(RegionalAlerteDTO::type)
                .containsExactly("DOSSIERS_BLOQUES", "ECHEANCES_DEPASSEES");
        assertThat(alertes.get(1).severite()).isEqualTo("critical");
        assertThat(alertes.get(1).count()).isEqualTo(1);
    }
}
