package com.consulos.rendezvous;

import com.consulos.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Component
public class RdvStatutEngine {

    private static final Map<RdvStatut, Set<RdvStatut>> TRANSITIONS = new EnumMap<>(RdvStatut.class);

    static {
        TRANSITIONS.put(RdvStatut.CONFIRME, Set.of(
                RdvStatut.PRESENT, RdvStatut.ABSENT, RdvStatut.ANNULE, RdvStatut.REPORTE
        ));
        TRANSITIONS.put(RdvStatut.EN_ATTENTE, Set.of(RdvStatut.CONFIRME, RdvStatut.ANNULE));
    }

    public void validateTransition(RdvStatut statutActuel, RdvStatut nouveauStatut) {
        Set<RdvStatut> allowed = TRANSITIONS.get(statutActuel);
        if (allowed == null || !allowed.contains(nouveauStatut)) {
            throw new BusinessException(
                    "TRANSITION_INVALIDE",
                    "Transition " + statutActuel + " → " + nouveauStatut + " non autorisée",
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}
