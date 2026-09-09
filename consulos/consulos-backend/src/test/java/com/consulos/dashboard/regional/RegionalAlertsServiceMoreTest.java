package com.consulos.dashboard.regional;

import com.consulos.auth.ConsulosUserDetails;
import com.consulos.auth.entity.OrganisationEntity;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegionalAlertsServiceMoreTest {

    @Mock
    private RegionalAlertsQueryRepository repository;
    @Mock
    private ReseauService reseauService;
    @Mock
    private ReseauOrganisationLookupRepository organisationLookupRepository;

    @InjectMocks
    private RegionalAlertsService regionalAlertsService;

    private UUID userId;
    private UUID orgA;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
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
    void getAlertes_whenReseauIdNull_shouldUseAllAllowedOrgs() {
        Set<UUID> allOrgs = Set.of(orgA);
        when(reseauService.resolveAllAllowedOrganisationIds(userId)).thenReturn(allOrgs);
        when(repository.dossiersBloquesEnRevue(allOrgs)).thenReturn(List.of());
        when(repository.dossiersEcheanceDepassee(allOrgs)).thenReturn(List.of());
        when(repository.agentsSurcharges(allOrgs)).thenReturn(List.of());
        when(repository.postesCaisseSilencieuse(eq(allOrgs), any())).thenReturn(List.of());

        List<RegionalAlerteDTO> alertes = regionalAlertsService.getAlertes(null);

        assertThat(alertes).isEmpty();
        verify(reseauService).resolveAllAllowedOrganisationIds(userId);
    }

    @Test
    void getAlertes_shouldIncludeAgentsSurcharges() {
        UUID reseauId = UUID.randomUUID();
        when(reseauService.resolveAllowedOrganisationIds(reseauId, userId)).thenReturn(Set.of(orgA));
        when(repository.dossiersBloquesEnRevue(Set.of(orgA))).thenReturn(List.of());
        when(repository.dossiersEcheanceDepassee(Set.of(orgA))).thenReturn(List.of());
        when(repository.agentsSurcharges(Set.of(orgA)))
                .thenReturn(List.<Object[]>of(new Object[]{orgA, UUID.randomUUID(), 25L}));
        OrganisationEntity org = new OrganisationEntity();
        org.setId(orgA);
        org.setCode("GNB-FR");
        when(organisationLookupRepository.findByIdIn(Set.of(orgA))).thenReturn(List.of(org));
        when(repository.postesCaisseSilencieuse(eq(Set.of(orgA)), any())).thenReturn(List.of());

        List<RegionalAlerteDTO> alertes = regionalAlertsService.getAlertes(reseauId);

        assertThat(alertes).hasSize(1);
        RegionalAlerteDTO alerte = alertes.getFirst();
        assertThat(alerte.type()).isEqualTo("AGENTS_SURCHARGES");
        assertThat(alerte.severite()).isEqualTo("warning");
        assertThat(alerte.count()).isEqualTo(1);
        assertThat(alerte.posteCodes()).containsExactly("GNB-FR");
        assertThat(alerte.dossiersIds()).isEmpty();
    }

    @Test
    void getAlertes_shouldIncludeCaisseSilencieuse() {
        UUID reseauId = UUID.randomUUID();
        when(reseauService.resolveAllowedOrganisationIds(reseauId, userId)).thenReturn(Set.of(orgA));
        when(repository.dossiersBloquesEnRevue(Set.of(orgA))).thenReturn(List.of());
        when(repository.dossiersEcheanceDepassee(Set.of(orgA))).thenReturn(List.of());
        when(repository.agentsSurcharges(Set.of(orgA))).thenReturn(List.of());
        when(repository.postesCaisseSilencieuse(eq(Set.of(orgA)), any()))
                .thenReturn(List.<Object[]>of(new Object[]{orgA, "GNB-BE"}));

        List<RegionalAlerteDTO> alertes = regionalAlertsService.getAlertes(reseauId);

        assertThat(alertes).hasSize(1);
        RegionalAlerteDTO alerte = alertes.getFirst();
        assertThat(alerte.type()).isEqualTo("CAISSE_SILENCIEUSE");
        assertThat(alerte.severite()).isEqualTo("warning");
        assertThat(alerte.count()).isEqualTo(1);
        assertThat(alerte.posteCodes()).containsExactly("GNB-BE");
        assertThat(alerte.dossiersIds()).isEmpty();
    }

    @Test
    void getAlertes_shouldTruncateEntityIdsAbove50() {
        UUID reseauId = UUID.randomUUID();
        List<UUID> bloques = IntStream.range(0, 55)
                .mapToObj(i -> UUID.randomUUID())
                .toList();
        when(reseauService.resolveAllowedOrganisationIds(reseauId, userId)).thenReturn(Set.of(orgA));
        when(repository.dossiersBloquesEnRevue(Set.of(orgA))).thenReturn(bloques);
        when(repository.posteCodesForDossierIds(bloques)).thenReturn(List.of("GNB-FR"));
        when(repository.dossiersEcheanceDepassee(Set.of(orgA))).thenReturn(List.of());
        when(repository.agentsSurcharges(Set.of(orgA))).thenReturn(List.of());
        when(repository.postesCaisseSilencieuse(eq(Set.of(orgA)), any())).thenReturn(List.of());

        List<RegionalAlerteDTO> alertes = regionalAlertsService.getAlertes(reseauId);

        assertThat(alertes).hasSize(1);
        RegionalAlerteDTO alerte = alertes.getFirst();
        assertThat(alerte.type()).isEqualTo("DOSSIERS_BLOQUES");
        assertThat(alerte.count()).isEqualTo(55);
        assertThat(alerte.dossiersIds()).hasSize(50);
        assertThat(alerte.dossiersIds()).isEqualTo(new ArrayList<>(bloques.subList(0, 50)));
    }
}
