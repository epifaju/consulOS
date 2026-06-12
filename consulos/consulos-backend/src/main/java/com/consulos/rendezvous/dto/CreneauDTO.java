package com.consulos.rendezvous.dto;

import java.time.LocalTime;

public record CreneauDTO(
        LocalTime heureDebut,
        LocalTime heureFin,
        int placesRestantes,
        int quotaMax
) {
}
