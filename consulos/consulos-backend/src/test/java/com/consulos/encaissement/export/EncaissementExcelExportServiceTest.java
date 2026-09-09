package com.consulos.encaissement.export;

import com.consulos.common.BusinessException;
import com.consulos.common.PageResponse;
import com.consulos.encaissement.EncaissementService;
import com.consulos.encaissement.ModePaiement;
import com.consulos.encaissement.StatutEncaissement;
import com.consulos.encaissement.dto.EncaissementSummaryDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EncaissementExcelExportServiceTest {

    @Mock
    private EncaissementService encaissementService;

    @InjectMocks
    private EncaissementExcelExportService exportService;

    private UUID organisationId;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
    }

    @Test
    void exportJournal_withOneRow_shouldReturnXlsxBytes() {
        LocalDate debut = LocalDate.of(2026, 9, 1);
        LocalDate fin = LocalDate.of(2026, 9, 30);
        EncaissementSummaryDTO summary = new EncaissementSummaryDTO(
                UUID.randomUUID(),
                "REC-2026-00001",
                UUID.randomUUID(),
                "Camara",
                "Aminata",
                UUID.randomUUID(),
                "DOS-2026-00042",
                "Passeport",
                new BigDecimal("86.00"),
                new BigDecimal("86.00"),
                "EUR",
                ModePaiement.ESPECES,
                StatutEncaissement.PAYE,
                OffsetDateTime.of(2026, 9, 15, 10, 30, 0, 0, ZoneOffset.UTC)
        );
        when(encaissementService.list(
                eq(organisationId),
                isNull(),
                isNull(),
                isNull(),
                any(OffsetDateTime.class),
                any(OffsetDateTime.class),
                any(Pageable.class)
        )).thenReturn(new PageResponse<>(List.of(summary), 1, 1, 0, 1, true, true));

        byte[] bytes = exportService.exportJournal(debut, fin, organisationId);

        assertThat(bytes).isNotEmpty();
        assertThat(bytes[0]).isEqualTo((byte) 'P');
        assertThat(bytes[1]).isEqualTo((byte) 'K');
    }

    @Test
    void exportJournal_withEmptyContent_shouldStillReturnXlsx() {
        LocalDate debut = LocalDate.of(2026, 9, 1);
        LocalDate fin = LocalDate.of(2026, 9, 30);
        when(encaissementService.list(
                eq(organisationId),
                isNull(),
                isNull(),
                isNull(),
                any(OffsetDateTime.class),
                any(OffsetDateTime.class),
                any(Pageable.class)
        )).thenReturn(new PageResponse<>(List.of(), 0, 0, 0, 0, true, true));

        byte[] bytes = exportService.exportJournal(debut, fin, organisationId);

        assertThat(bytes).isNotEmpty();
        assertThat(bytes[0]).isEqualTo((byte) 'P');
        assertThat(bytes[1]).isEqualTo((byte) 'K');
    }

    @Test
    void exportJournal_whenDateFinBeforeDateDebut_shouldThrowAndNeverList() {
        LocalDate debut = LocalDate.of(2026, 9, 30);
        LocalDate fin = LocalDate.of(2026, 9, 1);

        assertThatThrownBy(() -> exportService.exportJournal(debut, fin, organisationId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("INVALID_DATE_RANGE");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });

        verify(encaissementService, never()).list(
                any(), any(), any(), any(), any(), any(), any()
        );
    }
}
