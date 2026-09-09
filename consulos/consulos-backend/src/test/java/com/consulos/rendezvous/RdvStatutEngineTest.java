package com.consulos.rendezvous;

import com.consulos.common.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RdvStatutEngineTest {

    private RdvStatutEngine engine;

    @BeforeEach
    void setUp() {
        engine = new RdvStatutEngine();
    }

    @ParameterizedTest
    @CsvSource({
            "CONFIRME,PRESENT",
            "CONFIRME,ABSENT",
            "CONFIRME,ANNULE",
            "CONFIRME,REPORTE",
            "EN_ATTENTE,CONFIRME",
            "EN_ATTENTE,ANNULE"
    })
    void validateTransition_allowed_shouldPass(RdvStatut from, RdvStatut to) {
        assertThatCode(() -> engine.validateTransition(from, to)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({
            "CONFIRME,EN_ATTENTE",
            "CONFIRME,CONFIRME",
            "EN_ATTENTE,PRESENT",
            "EN_ATTENTE,ABSENT",
            "EN_ATTENTE,REPORTE",
            "PRESENT,CONFIRME",
            "ABSENT,ANNULE",
            "ANNULE,CONFIRME",
            "REPORTE,PRESENT"
    })
    void validateTransition_forbidden_shouldThrow(RdvStatut from, RdvStatut to) {
        assertThatThrownBy(() -> engine.validateTransition(from, to))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo("TRANSITION_INVALIDE");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(be.getMessage()).contains(from.name()).contains(to.name());
                });
    }

    @Test
    void terminalStatuses_haveNoOutgoingTransitions() {
        for (RdvStatut terminal : new RdvStatut[]{
                RdvStatut.PRESENT, RdvStatut.ABSENT, RdvStatut.ANNULE, RdvStatut.REPORTE
        }) {
            assertThatThrownBy(() -> engine.validateTransition(terminal, RdvStatut.CONFIRME))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo("TRANSITION_INVALIDE"));
        }
    }
}
