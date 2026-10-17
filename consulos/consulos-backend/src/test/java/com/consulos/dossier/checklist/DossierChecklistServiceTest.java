package com.consulos.dossier.checklist;

import com.consulos.common.BusinessException;
import com.consulos.dossier.DossierEntity;
import com.consulos.dossier.DossierRepository;
import com.consulos.dossier.TypeServiceEntity;
import com.consulos.dossier.TypeServiceRepository;
import com.consulos.dossier.checklist.dto.DossierChecklistItemDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DossierChecklistServiceTest {

    @Mock
    private DossierRepository dossierRepository;
    @Mock
    private TypeServiceRepository typeServiceRepository;
    @Mock
    private DossierChecklistStatutRepository statutRepository;

    @InjectMocks
    private DossierChecklistService service;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private UUID dossierId;
    private UUID organisationId;
    private UUID typeServiceId;
    private DossierEntity dossier;
    private TypeServiceEntity typeService;

    @BeforeEach
    void setUp() {
        dossierId = UUID.randomUUID();
        organisationId = UUID.randomUUID();
        typeServiceId = UUID.randomUUID();
        dossier = new DossierEntity();
        dossier.setId(dossierId);
        dossier.setOrganisationId(organisationId);
        dossier.setTypeServiceId(typeServiceId);
        typeService = new TypeServiceEntity();
        typeService.setId(typeServiceId);
        typeService.setOrganisationId(organisationId);
        typeService.setChecklist(template("ID", "Pièce d'identité", "PHOTO", "Photo"));
    }

    @Test
    void list_shouldMergeTemplateWithPersistedFlags() {
        stubDossierAndType();
        DossierChecklistStatutEntity photo = new DossierChecklistStatutEntity();
        photo.setCode("PHOTO");
        photo.setValide(true);
        when(statutRepository.findByDossierIdAndOrganisationId(dossierId, organisationId))
                .thenReturn(List.of(photo));

        List<DossierChecklistItemDTO> items = service.list(dossierId, organisationId);

        assertThat(items).hasSize(2);
        assertThat(items.getFirst().code()).isEqualTo("ID");
        assertThat(items.getFirst().valide()).isFalse();
        assertThat(items.get(1).code()).isEqualTo("PHOTO");
        assertThat(items.get(1).valide()).isTrue();
        assertThat(items.get(1).libelle()).isEqualTo("Photo");
    }

    @Test
    void update_shouldPersistNewStatut() {
        stubDossierAndType();
        when(statutRepository.findByDossierIdAndOrganisationIdAndCode(dossierId, organisationId, "ID"))
                .thenReturn(Optional.empty());
        when(statutRepository.save(any(DossierChecklistStatutEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(statutRepository.findByDossierIdAndOrganisationId(dossierId, organisationId))
                .thenReturn(List.of());

        service.update(dossierId, "ID", true, organisationId);

        ArgumentCaptor<DossierChecklistStatutEntity> captor =
                ArgumentCaptor.forClass(DossierChecklistStatutEntity.class);
        verify(statutRepository).save(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("ID");
        assertThat(captor.getValue().getValide()).isTrue();
        assertThat(captor.getValue().getDossierId()).isEqualTo(dossierId);
        assertThat(captor.getValue().getOrganisationId()).isEqualTo(organisationId);
    }

    @Test
    void update_whenUnknownCode_shouldThrow() {
        stubDossierAndType();

        assertThatThrownBy(() -> service.update(dossierId, "INCONNU", true, organisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("CHECKLIST_ITEM_UNKNOWN");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                });
    }

    @Test
    void list_whenDossierMissing_shouldThrowNotFound() {
        when(dossierRepository.findByIdAndOrganisationIdAndActifTrue(dossierId, organisationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.list(dossierId, organisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("DOSSIER_NOT_FOUND"));
    }

    private void stubDossierAndType() {
        when(dossierRepository.findByIdAndOrganisationIdAndActifTrue(dossierId, organisationId))
                .thenReturn(Optional.of(dossier));
        when(typeServiceRepository.findByIdAndOrganisationIdAndActifTrue(typeServiceId, organisationId))
                .thenReturn(Optional.of(typeService));
    }

    private ArrayNode template(String... codeLibellePairs) {
        ArrayNode array = objectMapper.createArrayNode();
        for (int i = 0; i < codeLibellePairs.length; i += 2) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("code", codeLibellePairs[i]);
            node.put("libelle", codeLibellePairs[i + 1]);
            array.add(node);
        }
        return array;
    }
}
