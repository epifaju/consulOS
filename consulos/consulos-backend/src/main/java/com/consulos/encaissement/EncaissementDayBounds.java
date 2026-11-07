package com.consulos.encaissement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Bornes d'un jour civil dans le fuseau JVM (caisse / journal).
 * {@code createdAt} est un {@link LocalDateTime} : filtrer en UTC minuit décale
 * les encaissements de 0h–1h (CET) hors de « aujourd'hui ».
 */
public final class EncaissementDayBounds {

    private EncaissementDayBounds() {
    }

    public static OffsetDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
    }

    public static OffsetDateTime startOfNextDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
    }

    public static LocalDateTime toLocal(OffsetDateTime instant) {
        return instant.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }
}
